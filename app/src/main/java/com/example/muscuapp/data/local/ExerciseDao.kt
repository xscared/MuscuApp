package com.example.muscuapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // --- Sessions ---
    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutWithExercisesAndSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    suspend fun getAllWorkoutsOnce(): List<WorkoutWithExercisesAndSets>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(session: WorkoutSessionEntity): Long

    @Delete
    suspend fun deleteWorkout(session: WorkoutSessionEntity)

    @Update
    suspend fun updateWorkout(session: WorkoutSessionEntity)

    // --- Exercises ---
    @Query("SELECT * FROM exercises WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getExercisesForSession(sessionId: Long): Flow<List<ExerciseEntity>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getExercisesWithSetsForSession(sessionId: Long): Flow<List<ExerciseWithSets>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("SELECT exerciseDefinitionId FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseDefinitionIdById(exerciseId: Long): String?

    @Query("SELECT id FROM exercises WHERE exerciseDefinitionId = :exerciseDefinitionId")
    suspend fun getExerciseIdsByDefinition(exerciseDefinitionId: String): List<Long>

    @Query("UPDATE exercises SET name = :name, category = :category WHERE exerciseDefinitionId = :exerciseDefinitionId")
    suspend fun syncExerciseDetailsByDefinition(exerciseDefinitionId: String, name: String, category: String)

    @Query("UPDATE exercise_sets SET weight = :weight, reps = :reps WHERE `order` = :order AND isWarmup = :isWarmup AND exerciseId IN (SELECT id FROM exercises WHERE exerciseDefinitionId = :exerciseDefinitionId)")
    suspend fun syncSetByDefinitionOrderAndWarmup(exerciseDefinitionId: String, order: Int, isWarmup: Boolean, weight: Float, reps: Int)

    @Query("""
        UPDATE exercises
        SET
            sets = (SELECT COUNT(*) FROM exercise_sets WHERE exerciseId = exercises.id AND isWarmup = 0),
            weight = COALESCE((SELECT MAX(weight) FROM exercise_sets WHERE exerciseId = exercises.id AND isWarmup = 0), 0),
            reps = COALESCE((
                SELECT MAX(reps)
                FROM exercise_sets
                WHERE exerciseId = exercises.id
                    AND isWarmup = 0
                    AND weight = COALESCE((SELECT MAX(weight) FROM exercise_sets WHERE exerciseId = exercises.id AND isWarmup = 0), 0)
            ), 0)
        WHERE exerciseDefinitionId = :exerciseDefinitionId
    """)
    suspend fun refreshExerciseSummariesByDefinition(exerciseDefinitionId: String)

    @Transaction
    suspend fun updateExerciseWithSync(exercise: ExerciseEntity) {
        updateExercise(exercise)
        syncExerciseDetailsByDefinition(exercise.exerciseDefinitionId, exercise.name, exercise.category)
    }

    @Query("SELECT MAX(weight) FROM exercises WHERE name = :name")
    suspend fun getPersonalRecord(name: String): Float?

    // --- Sets ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun getSetsForExerciseOnce(exerciseId: Long): List<ExerciseSetEntity>

    @Update
    suspend fun updateSet(set: ExerciseSetEntity)

    @Transaction
    suspend fun updateSetWithSync(set: ExerciseSetEntity) {
        updateSet(set)
        val exerciseDefinitionId = getExerciseDefinitionIdById(set.exerciseId)
        if (exerciseDefinitionId != null) {
            syncSetByDefinitionOrderAndWarmup(exerciseDefinitionId, set.order, set.isWarmup, set.weight, set.reps)
            refreshExerciseSummariesByDefinition(exerciseDefinitionId)
        }
    }

    @Transaction
    suspend fun replaceExerciseSetsWithSync(
        exerciseId: Long,
        desiredSets: List<ExerciseSetEntity>
    ) {
        val exerciseDefinitionId = getExerciseDefinitionIdById(exerciseId) ?: return
        val desiredRegularSets = desiredSets.filterNot { it.isWarmup }
        val desiredWarmupSets = desiredSets.filter { it.isWarmup }

        getExerciseIdsByDefinition(exerciseDefinitionId).forEach { linkedExerciseId ->
            val existingSets = getSetsForExerciseOnce(linkedExerciseId)
            reconcileSetType(
                linkedExerciseId = linkedExerciseId,
                desiredSets = desiredRegularSets,
                existingSets = existingSets.filterNot { it.isWarmup },
                isWarmup = false
            )
            reconcileSetType(
                linkedExerciseId = linkedExerciseId,
                desiredSets = desiredWarmupSets,
                existingSets = existingSets.filter { it.isWarmup },
                isWarmup = true
            )
        }

        refreshExerciseSummariesByDefinition(exerciseDefinitionId)
    }

    private suspend fun reconcileSetType(
        linkedExerciseId: Long,
        desiredSets: List<ExerciseSetEntity>,
        existingSets: List<ExerciseSetEntity>,
        isWarmup: Boolean
    ) {
        val sortedExistingSets = existingSets.sortedWith(
            compareBy<ExerciseSetEntity> { it.order }
                .thenBy { it.timestamp }
                .thenBy { it.setId }
        )

        desiredSets.forEachIndexed { index, desiredSet ->
            val existingSet = sortedExistingSets.getOrNull(index)
            if (existingSet == null) {
                insertSet(
                    ExerciseSetEntity(
                        exerciseId = linkedExerciseId,
                        reps = desiredSet.reps,
                        weight = desiredSet.weight,
                        isWarmup = isWarmup,
                        order = index
                    )
                )
            } else {
                updateSet(
                    existingSet.copy(
                        reps = desiredSet.reps,
                        weight = desiredSet.weight,
                        isWarmup = isWarmup,
                        order = index
                    )
                )
            }
        }

        sortedExistingSets
            .drop(desiredSets.size)
            .forEach { deleteSet(it) }
    }

    @Delete
    suspend fun deleteSet(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun deleteAllSetsForExercise(exerciseId: Long)

    // --- Templates ---
    @Transaction
    @Query("SELECT * FROM workout_templates")
    fun getAllTemplates(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates")
    suspend fun getAllTemplatesOnce(): List<TemplateWithExercises>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(exercise: TemplateExerciseEntity): Long

    // --- Import / Export ---
    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM workout_templates")
    suspend fun clearTemplates()

    @Transaction
    suspend fun clearAllData() {
        clearSessions()
        clearTemplates()
    }

    @Transaction
    suspend fun restoreDatabase(
        sessions: List<WorkoutSessionEntity>,
        exercises: List<ExerciseEntity>,
        sets: List<ExerciseSetEntity>,
        templates: List<WorkoutTemplateEntity>,
        templateExercises: List<TemplateExerciseEntity>,
    ) {
        clearAllData()
        
        insertSessionsRaw(sessions)
        insertExercisesRaw(exercises)
        insertSetsRaw(sets)
        insertTemplatesRaw(templates)
        insertTemplateExercisesRaw(templateExercises)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionsRaw(sessions: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercisesRaw(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetsRaw(sets: List<ExerciseSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplatesRaw(templates: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercisesRaw(templateExercises: List<TemplateExerciseEntity>)
}

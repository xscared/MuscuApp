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

    @Query("SELECT name FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseNameById(exerciseId: Long): String?

    @Query("UPDATE exercises SET weight = :weight, reps = :reps WHERE name = :name")
    suspend fun syncExerciseDataByName(name: String, weight: Float, reps: Int)

    @Query("UPDATE exercises SET weight = :weight, reps = :reps, sets = :sets, category = :category, note = :note WHERE name = :name")
    suspend fun syncExerciseAllDataByName(name: String, weight: Float, reps: Int, sets: Int, category: String, note: String)

    @Query("UPDATE exercise_sets SET weight = :weight, reps = :reps WHERE exerciseId IN (SELECT id FROM exercises WHERE name = :name)")
    suspend fun syncSetsByExerciseName(name: String, weight: Float, reps: Int)

    @Query("UPDATE template_exercises SET defaultWeight = :weight, defaultReps = :reps, defaultSets = :sets, category = :category WHERE name = :name")
    suspend fun syncTemplatesByName(name: String, weight: Float, reps: Int, sets: Int, category: String)

    @Query("UPDATE template_exercises SET defaultWeight = :weight, defaultReps = :reps WHERE name = :name")
    suspend fun syncTemplatesValuesByName(name: String, weight: Float, reps: Int)

    @Transaction
    suspend fun updateExerciseWithSync(exercise: ExerciseEntity) {
        updateExercise(exercise)
        syncExerciseAllDataByName(exercise.name, exercise.weight, exercise.reps, exercise.sets, exercise.category, exercise.note)
        syncSetsByExerciseName(exercise.name, exercise.weight, exercise.reps)
        syncTemplatesByName(exercise.name, exercise.weight, exercise.reps, exercise.sets, exercise.category)
    }

    @Query("SELECT MAX(weight) FROM exercises WHERE name = :name")
    suspend fun getPersonalRecord(name: String): Float?

    // --- Sets ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Update
    suspend fun updateSet(set: ExerciseSetEntity)

    @Transaction
    suspend fun updateSetWithSync(set: ExerciseSetEntity) {
        updateSet(set)
        val name = getExerciseNameById(set.exerciseId)
        if (name != null) {
            syncExerciseDataByName(name, set.weight, set.reps)
            syncSetsByExerciseName(name, set.weight, set.reps)
            syncTemplatesValuesByName(name, set.weight, set.reps)
        }
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
    @Query("SELECT * FROM workout_sessions")
    suspend fun dumpSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM exercises")
    suspend fun dumpExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise_sets")
    suspend fun dumpSets(): List<ExerciseSetEntity>

    @Query("SELECT * FROM workout_templates")
    suspend fun dumpTemplates(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM template_exercises")
    suspend fun dumpTemplateExercises(): List<TemplateExerciseEntity>

    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM workout_templates")
    suspend fun clearTemplates()

    @Transaction
    suspend fun clearAllData() {
        clearSessions()
        clearTemplates()
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

    @Transaction
    suspend fun restoreDatabase(
        sessions: List<WorkoutSessionEntity>,
        exercises: List<ExerciseEntity>,
        sets: List<ExerciseSetEntity>,
        templates: List<WorkoutTemplateEntity>,
        templateExercises: List<TemplateExerciseEntity>
    ) {
        clearSessions() // Cascades exercises and sets due to ForeignKey
        clearTemplates() // Cascades templateExercises due to ForeignKey
        
        insertSessionsRaw(sessions)
        insertExercisesRaw(exercises)
        insertSetsRaw(sets)
        insertTemplatesRaw(templates)
        insertTemplateExercisesRaw(templateExercises)
    }
}

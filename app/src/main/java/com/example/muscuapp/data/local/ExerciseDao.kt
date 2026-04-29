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

    @Query("SELECT MAX(weight) FROM exercises WHERE name = :name")
    suspend fun getPersonalRecord(name: String): Float?

    // --- Sets ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Update
    suspend fun updateSet(set: ExerciseSetEntity)

    @Delete
    suspend fun deleteSet(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun deleteAllSetsForExercise(exerciseId: Long)

    // --- Templates ---
    @Transaction
    @Query("SELECT * FROM workout_templates")
    fun getAllTemplates(): Flow<List<TemplateWithExercises>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(exercise: TemplateExerciseEntity): Long
}

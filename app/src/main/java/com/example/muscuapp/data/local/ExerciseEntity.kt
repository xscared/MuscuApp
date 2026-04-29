package com.example.muscuapp.data.local

import androidx.room.*

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val title: String,
    val date: Long = System.currentTimeMillis(),
    val isLive: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Float,
    val category: String = "Autre",
    val note: String = "",
    val isFavorite: Boolean = false,
    val isPR: Boolean = false,
    val order: Int = 0,
    val date: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true) val setId: Long = 0,
    val exerciseId: Long,
    val reps: Int,
    val weight: Float,
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false, // Nouveau champ
    val timestamp: Long = System.currentTimeMillis()
)

data class ExerciseWithSets(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<ExerciseSetEntity>
)

data class WorkoutWithExercisesAndSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseEntity::class,
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val exercises: List<ExerciseWithSets>
)

package com.example.muscuapp.data.local

import androidx.room.*

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val templateId: Long = 0,
    val name: String
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val name: String,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeight: Float,
    val category: String = "Autre"
)

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        parentColumn = "templateId",
        entityColumn = "templateId"
    )
    val exercises: List<TemplateExerciseEntity>
)

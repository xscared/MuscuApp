package com.example.muscuapp.data.backup

import com.example.muscuapp.data.local.ExerciseEntity
import com.example.muscuapp.data.local.ExerciseSetEntity
import com.example.muscuapp.data.local.TemplateExerciseEntity
import com.example.muscuapp.data.local.WorkoutSessionEntity
import com.example.muscuapp.data.local.WorkoutTemplateEntity

data class MuscuBackupFile(
    val version: Int = 1,
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val sets: List<ExerciseSetEntity> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val templateExercises: List<TemplateExerciseEntity> = emptyList(),
    val weightUnit: String = "KG",
    val preferredWorkoutIdsByDay: Map<Int, Long?> = emptyMap()
)
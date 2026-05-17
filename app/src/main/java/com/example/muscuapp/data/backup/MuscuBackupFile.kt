package com.example.muscuapp.data.backup

import com.example.muscuapp.data.local.*

data class MuscuBackupFile(
    val version: Int = 2,
    // New hierarchical structure (v2)
    val workouts: List<WorkoutBackupDto> = emptyList(),
    val templates: List<TemplateBackupDto> = emptyList(),
    val weightUnit: String = "KG",
    val preferredWorkoutNamesByDay: Map<Int, String?> = emptyMap(),
    
    // Legacy flat structure (v1) - kept for backward compatibility
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val sets: List<ExerciseSetEntity> = emptyList(),
    val templatesLegacy: List<WorkoutTemplateEntity> = emptyList(),
    val templateExercises: List<TemplateExerciseEntity> = emptyList(),
    val preferredWorkoutIdsByDay: Map<Int, Long?> = emptyMap()
)

data class WorkoutBackupDto(
    val title: String,
    val date: Long,
    val isLive: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val exercises: List<ExerciseBackupDto> = emptyList()
)

data class ExerciseBackupDto(
    val name: String,
    val setsCount: Int,
    val repsCount: Int,
    val weight: Float,
    val category: String = "Autre",
    val note: String = "",
    val isFavorite: Boolean = false,
    val isPR: Boolean = false,
    val order: Int = 0,
    val date: Long,
    val detailedSets: List<SetBackupDto> = emptyList()
)

data class SetBackupDto(
    val reps: Int,
    val weight: Float,
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false,
    val timestamp: Long,
    val order: Int = 0
)

data class TemplateBackupDto(
    val name: String,
    val exercises: List<TemplateExerciseBackupDto> = emptyList()
)

data class TemplateExerciseBackupDto(
    val name: String,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeight: Float,
    val category: String = "Autre"
)

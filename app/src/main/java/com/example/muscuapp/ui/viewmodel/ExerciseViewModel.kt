package com.example.muscuapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muscuapp.data.local.*
import com.example.muscuapp.data.backup.MuscuBackupManager
import com.example.muscuapp.data.prefs.ThemeMode
import com.example.muscuapp.data.prefs.UserPrefs
import com.example.muscuapp.data.prefs.WeightUnit
import com.example.muscuapp.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetInfo(
    val reps: Int,
    val weight: Float,
    val isWarmup: Boolean = false
)

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val userPrefs: UserPrefs,
    private val backupManager: MuscuBackupManager
) : ViewModel() {

    val workouts = repository.getAllWorkouts()
    val templates = repository.getAllTemplates()
    val weightUnit = userPrefs.weightUnit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)
    val themeMode = userPrefs.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val hapticEnabled = userPrefs.hapticEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hapticIntensity = userPrefs.hapticIntensity.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val alarmSound = userPrefs.alarmSound.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val preferredWorkoutIdsByDay = userPrefs.preferredWorkoutIdsByDay.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    fun createWorkout(title: String, isLive: Boolean = false, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createWorkout(title, isLive)
            onCreated(id)
        }
    }

    fun renameWorkout(session: WorkoutSessionEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateWorkout(session.copy(title = newTitle))
        }
    }

    fun deleteWorkout(session: WorkoutSessionEntity) {
        viewModelScope.launch {
            repository.deleteWorkout(session)
        }
    }

    fun startLiveWorkout(sessionId: Long, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            val workout = repository.getAllWorkouts().first().find { it.session.sessionId == sessionId }
            if (workout != null) {
                repository.updateWorkout(workout.session.copy(
                    isLive = true,
                    startTime = System.currentTimeMillis()
                ))
                // On s'assure que les séries existent
                workout.exercises.forEach { exWithSets ->
                    if (exWithSets.sets.isEmpty()) {
                        repeat(exWithSets.exercise.sets) { i ->
                            repository.insertSet(ExerciseSetEntity(
                                exerciseId = exWithSets.exercise.id,
                                reps = exWithSets.exercise.reps,
                                weight = exWithSets.exercise.weight,
                                order = i
                            ))
                        }
                    }
                    onStarted()
                }
            }
        }
    }

    fun addWarmupSet(exerciseId: Long, reps: Int, weight: Float) {
        viewModelScope.launch {
            repository.insertSet(ExerciseSetEntity(
                exerciseId = exerciseId,
                reps = reps,
                weight = weight,
                isWarmup = true,
                order = -1 
            ))
        }
    }

    fun addExerciseWithDetailedSets(
        sessionId: Long,
        name: String,
        category: String,
        note: String,
        sets: List<SetInfo>
    ) {
        viewModelScope.launch {
            // On calcule le poids max pour le PR et l'affichage général
            val maxWeight = sets.filter { !it.isWarmup }.maxOfOrNull { it.weight } ?: 0f
            val repsForMax = sets.filter { it.weight == maxWeight }.maxOfOrNull { it.reps } ?: 0
            
            val currentPR = repository.getPersonalRecord(name) ?: 0f
            val isPR = maxWeight > currentPR
            
            val currentExercises = repository.getExercisesForSession(sessionId).first()
            val nextOrder = (currentExercises.maxOfOrNull { it.order } ?: -1) + 1

            val exerciseId = repository.insertExercise(
                ExerciseEntity(
                    sessionId = sessionId,
                    name = name,
                    sets = sets.count { !it.isWarmup },
                    reps = repsForMax,
                    weight = maxWeight,
                    category = category,
                    note = note,
                    isPR = isPR,
                    order = nextOrder
                )
            )
            
            // Insertion de toutes les séries détaillées
            sets.forEachIndexed { index, setInfo ->
                repository.insertSet(
                    ExerciseSetEntity(
                        exerciseId = exerciseId,
                        reps = setInfo.reps,
                        weight = setInfo.weight,
                        isWarmup = setInfo.isWarmup,
                        order = index
                    )
                )
            }
        }
    }

    fun updateExerciseWithDetailedSets(
        exercise: ExerciseEntity,
        sets: List<SetInfo>
    ) {
        viewModelScope.launch {
            val maxWeight = sets.filter { !it.isWarmup }.maxOfOrNull { it.weight } ?: 0f
            val repsForMax = sets.filter { it.weight == maxWeight }.maxOfOrNull { it.reps } ?: 0
            
            val updatedExercise = exercise.copy(
                sets = sets.count { !it.isWarmup },
                reps = repsForMax,
                weight = maxWeight
            )
            repository.updateExercise(updatedExercise)
            
            // Mise à jour des séries : on supprime et on remplace (plus simple pour le moment)
            repository.deleteAllSetsForExercise(exercise.id)
            sets.forEachIndexed { index, setInfo ->
                repository.insertSet(
                    ExerciseSetEntity(
                        exerciseId = exercise.id,
                        reps = setInfo.reps,
                        weight = setInfo.weight,
                        isWarmup = setInfo.isWarmup,
                        order = index
                    )
                )
            }
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun reorderExercises(exercises: List<ExerciseEntity>) {
        viewModelScope.launch {
            try {
                exercises.forEachIndexed { index, exercise ->
                    repository.updateExercise(exercise.copy(order = index))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(exercise: ExerciseEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateExercise(exercise.copy(isFavorite = isFavorite))
        }
    }

    // --- Live Mode Actions ---
    fun toggleSetCompletion(set: ExerciseSetEntity, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateSet(set.copy(isCompleted = isCompleted))
        }
    }

    fun deleteSet(set: ExerciseSetEntity) {
        viewModelScope.launch {
            repository.deleteSet(set)
        }
    }

    fun updateSet(set: ExerciseSetEntity) {
        viewModelScope.launch {
            repository.updateSet(set)
        }
    }

    fun endWorkout(session: WorkoutSessionEntity, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorkout(session.copy(isLive = false, endTime = System.currentTimeMillis()))
            repository.getExercisesWithSetsForSession(session.sessionId).first().forEach { exerciseWithSets ->
                exerciseWithSets.sets.forEach { set ->
                    if (set.isCompleted) {
                        repository.updateSet(set.copy(isCompleted = false))
                    }
                }
            }
            onFinished()
        }
    }

    fun toggleWeightUnit() {
        viewModelScope.launch {
            val newUnit = if (weightUnit.value == WeightUnit.KG) WeightUnit.LBS else WeightUnit.KG
            userPrefs.setWeightUnit(newUnit)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPrefs.setThemeMode(mode)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setHapticEnabled(enabled)
        }
    }

    fun setHapticIntensity(intensity: Float) {
        viewModelScope.launch {
            userPrefs.setHapticIntensity(intensity)
        }
    }

    fun setAlarmSound(uri: String?) {
        viewModelScope.launch {
            userPrefs.setAlarmSound(uri)
        }
    }

    fun setPreferredWorkoutForDay(dayOfWeek: Int, workoutId: Long?) {
        viewModelScope.launch {
            userPrefs.setPreferredWorkoutForDay(dayOfWeek, workoutId)
        }
    }

    fun createWorkoutFromTemplate(template: TemplateWithExercises) {
        viewModelScope.launch {
            repository.createWorkoutFromTemplate(template)
        }
    }

    // --- Import / Export ---
    fun exportBackup(uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.createBackup(uri)
            onResult(result.isSuccess)
        }
    }

    fun importBackup(uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreBackup(uri)
            onResult(result.isSuccess)
        }
    }
}

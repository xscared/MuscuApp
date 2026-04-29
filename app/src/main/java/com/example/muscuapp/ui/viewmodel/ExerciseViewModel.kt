package com.example.muscuapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muscuapp.data.local.*
import com.example.muscuapp.data.prefs.UserPrefs
import com.example.muscuapp.data.prefs.WeightUnit
import com.example.muscuapp.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SetInfo(
    val reps: Int,
    val weight: Float,
    val isWarmup: Boolean = false
)

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    val workouts = repository.getAllWorkouts()
    val templates = repository.getAllTemplates()
    val weightUnit = userPrefs.weightUnit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    // Calcul des Streaks (semaines consécutives avec au moins 2 séances)
    val streakCount: StateFlow<Int> = workouts.map { sessionList ->
        calculateStreaks(sessionList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun calculateStreaks(workouts: List<WorkoutWithExercisesAndSets>): Int {
        if (workouts.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        val workoutsByWeek = workouts.groupBy {
            calendar.timeInMillis = it.session.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
        }
        var streaks = 0
        val currentWeek = Calendar.getInstance()
        while (true) {
            val key = "${currentWeek.get(Calendar.YEAR)}-${currentWeek.get(Calendar.WEEK_OF_YEAR)}"
            val sessionsInWeek = workoutsByWeek[key]?.size ?: 0
            if (sessionsInWeek >= 2) {
                streaks++
                currentWeek.add(Calendar.WEEK_OF_YEAR, -1)
            } else {
                val now = Calendar.getInstance()
                if (currentWeek.get(Calendar.YEAR) == now.get(Calendar.YEAR) && 
                    currentWeek.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                    currentWeek.add(Calendar.WEEK_OF_YEAR, -1)
                    continue 
                }
                break
            }
        }
        return streaks
    }

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

    fun startLiveWorkout(sessionId: Long) {
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
                        repeat(exWithSets.exercise.sets) {
                            repository.insertSet(ExerciseSetEntity(
                                exerciseId = exWithSets.exercise.id,
                                reps = exWithSets.exercise.reps,
                                weight = exWithSets.exercise.weight
                            ))
                        }
                    }
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
                isWarmup = true
            ))
        }
    }

    fun getExercisesWithSets(sessionId: Long): Flow<List<ExerciseWithSets>> =
        repository.getExercisesWithSetsForSession(sessionId)

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
            sets.forEach { setInfo ->
                repository.insertSet(
                    ExerciseSetEntity(
                        exerciseId = exerciseId,
                        reps = setInfo.reps,
                        weight = setInfo.weight,
                        isWarmup = setInfo.isWarmup
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
            sets.forEach { setInfo ->
                repository.insertSet(
                    ExerciseSetEntity(
                        exerciseId = exercise.id,
                        reps = setInfo.reps,
                        weight = setInfo.weight,
                        isWarmup = setInfo.isWarmup
                    )
                )
            }
        }
    }

    fun addExercise(sessionId: Long, name: String, sets: Int, reps: Int, weight: Float, category: String, note: String) {
        viewModelScope.launch {
            val currentPR = repository.getPersonalRecord(name) ?: 0f
            val isPR = weight > currentPR
            
            val currentExercises = repository.getExercisesForSession(sessionId).first()
            val nextOrder = (currentExercises.maxOfOrNull { it.order } ?: -1) + 1

            val exerciseId = repository.insertExercise(
                ExerciseEntity(
                    sessionId = sessionId,
                    name = name,
                    sets = sets,
                    reps = reps,
                    weight = weight,
                    category = category,
                    note = note,
                    isPR = isPR,
                    order = nextOrder
                )
            )
            
            val workout = repository.getAllWorkouts().first().find { it.session.sessionId == sessionId }
            if (workout?.session?.isLive == true) {
                repeat(sets) {
                    repository.insertSet(ExerciseSetEntity(exerciseId = exerciseId, reps = reps, weight = weight))
                }
            }
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

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun updateExerciseDetails(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
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

    fun createWorkoutFromTemplate(template: TemplateWithExercises) {
        viewModelScope.launch {
            repository.createWorkoutFromTemplate(template)
        }
    }

    fun exportCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = repository.getCsvData()
            onResult(data)
        }
    }
}

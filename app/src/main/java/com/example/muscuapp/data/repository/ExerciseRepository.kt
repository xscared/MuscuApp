package com.example.muscuapp.data.repository

import com.example.muscuapp.data.local.*
import com.example.muscuapp.data.backup.MuscuBackupFile
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class ExerciseRepository(private val dao: ExerciseDao) {
    
    fun getAllWorkouts(): Flow<List<WorkoutWithExercisesAndSets>> = dao.getAllWorkouts()

    suspend fun createWorkout(title: String, isLive: Boolean = false): Long {
        return dao.insertWorkout(WorkoutSessionEntity(
            title = title, 
            isLive = isLive,
            startTime = if (isLive) System.currentTimeMillis() else null
        ))
    }

    suspend fun deleteWorkout(session: WorkoutSessionEntity) {
        dao.deleteWorkout(session)
    }

    suspend fun updateWorkout(session: WorkoutSessionEntity) {
        dao.updateWorkout(session)
    }

    fun getExercisesForSession(sessionId: Long): Flow<List<ExerciseEntity>> = 
        dao.getExercisesForSession(sessionId)

    fun getExercisesWithSetsForSession(sessionId: Long): Flow<List<ExerciseWithSets>> =
        dao.getExercisesWithSetsForSession(sessionId)

    suspend fun insertExercise(exercise: ExerciseEntity): Long {
        return dao.insertExercise(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        dao.deleteExercise(exercise)
    }

    suspend fun updateExercise(exercise: ExerciseEntity) {
        dao.updateExercise(exercise)
    }

    suspend fun getPersonalRecord(name: String): Float? {
        return dao.getPersonalRecord(name)
    }

    // --- Sets ---
    suspend fun insertSet(set: ExerciseSetEntity) = dao.insertSet(set)
    suspend fun updateSet(set: ExerciseSetEntity) = dao.updateSet(set)
    suspend fun deleteSet(set: ExerciseSetEntity) = dao.deleteSet(set)
    suspend fun deleteAllSetsForExercise(exerciseId: Long) = dao.deleteAllSetsForExercise(exerciseId)

    // Templates
    fun getAllTemplates(): Flow<List<TemplateWithExercises>> = dao.getAllTemplates()

    suspend fun createTemplate(name: String, exercises: List<TemplateExerciseEntity>) {
        val templateId = dao.insertTemplate(WorkoutTemplateEntity(name = name))
        exercises.forEach { 
            dao.insertTemplateExercise(it.copy(templateId = templateId))
        }
    }

    suspend fun createWorkoutFromTemplate(template: TemplateWithExercises) {
        val sessionId = dao.insertWorkout(WorkoutSessionEntity(title = template.template.name))
        template.exercises.forEach { templateEx ->
            dao.insertExercise(
                ExerciseEntity(
                    sessionId = sessionId,
                    name = templateEx.name,
                    sets = templateEx.defaultSets,
                    reps = templateEx.defaultReps,
                    weight = templateEx.defaultWeight,
                    category = templateEx.category
                )
            )
        }
    }

    // Export CSV
    suspend fun getCsvData(): String {
        val workouts = dao.getAllWorkoutsOnce()
        val csv = StringBuilder("Séance,Date,Exercice,Catégorie,Séries,Reps,Poids(kg),Note,PR\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        workouts.forEach { w ->
            w.exercises.forEach { exWithSets ->
                val e = exWithSets.exercise
                csv.append("${w.session.title},")
                csv.append("${dateFormat.format(Date(w.session.date))},")
                csv.append("${e.name},")
                csv.append("${e.category},")
                csv.append("${e.sets},")
                csv.append("${e.reps},")
                csv.append("${e.weight},")
                csv.append("${e.note.replace(",", " ")},")
                csv.append("${if (e.isPR) "OUI" else "NON"}\n")
            }
        }
        return csv.toString()
    }

    suspend fun getMuscuBackupData(): MuscuBackupFile {
        return MuscuBackupFile(
            sessions = dao.dumpSessions(),
            exercises = dao.dumpExercises(),
            sets = dao.dumpSets(),
            templates = dao.dumpTemplates(),
            templateExercises = dao.dumpTemplateExercises()
        )
    }

    suspend fun restoreMuscuBackupData(backup: MuscuBackupFile) {
        dao.restoreDatabase(
            sessions = backup.sessions,
            exercises = backup.exercises,
            sets = backup.sets,
            templates = backup.templates,
            templateExercises = backup.templateExercises
        )
    }
}

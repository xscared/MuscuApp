package com.example.muscuapp.data.repository

import com.example.muscuapp.data.local.*
import com.example.muscuapp.data.backup.*
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {
    
    fun getAllWorkouts(): Flow<List<WorkoutWithExercisesAndSets>> = dao.getAllWorkouts()

    suspend fun createWorkout(title: String, isLive: Boolean = false): Long {
        return dao.insertWorkout(
            WorkoutSessionEntity(
                title = title,
                isLive = isLive,
                startTime = if (isLive) System.currentTimeMillis() else null,
            )
        )
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
        dao.updateExerciseWithSync(exercise)
    }

    suspend fun getPersonalRecord(name: String): Float? {
        return dao.getPersonalRecord(name)
    }

    // --- Sets ---
    suspend fun insertSet(set: ExerciseSetEntity) = dao.insertSet(set)
    suspend fun updateSet(set: ExerciseSetEntity) = dao.updateSetWithSync(set)
    suspend fun deleteSet(set: ExerciseSetEntity) = dao.deleteSet(set)
    suspend fun deleteAllSetsForExercise(exerciseId: Long) = dao.deleteAllSetsForExercise(exerciseId)

    // Templates
    fun getAllTemplates(): Flow<List<TemplateWithExercises>> = dao.getAllTemplates()

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

    suspend fun getMuscuBackupData(): MuscuBackupFile {
        val allWorkouts = dao.getAllWorkoutsOnce()
        val allTemplates = dao.getAllTemplatesOnce()
        
        return MuscuBackupFile(
            workouts = allWorkouts.map { w ->
                WorkoutBackupDto(
                    title = w.session.title,
                    date = w.session.date,
                    isLive = w.session.isLive,
                    startTime = w.session.startTime,
                    endTime = w.session.endTime,
                    exercises = w.exercises.map { e ->
                        ExerciseBackupDto(
                            name = e.exercise.name,
                            setsCount = e.exercise.sets,
                            repsCount = e.exercise.reps,
                            weight = e.exercise.weight,
                            category = e.exercise.category,
                            note = e.exercise.note,
                            isFavorite = e.exercise.isFavorite,
                            isPR = e.exercise.isPR,
                            order = e.exercise.order,
                            date = e.exercise.date,
                            detailedSets = e.sets.map { s ->
                                SetBackupDto(
                                    reps = s.reps,
                                    weight = s.weight,
                                    isCompleted = s.isCompleted,
                                    isWarmup = s.isWarmup,
                                    timestamp = s.timestamp,
                                    order = s.order
                                )
                            }
                        )
                    }
                )
            },
            templates = allTemplates.map { t ->
                TemplateBackupDto(
                    name = t.template.name,
                    exercises = t.exercises.map { te ->
                        TemplateExerciseBackupDto(
                            name = te.name,
                            defaultSets = te.defaultSets,
                            defaultReps = te.defaultReps,
                            defaultWeight = te.defaultWeight,
                            category = te.category
                        )
                    }
                )
            }
        )
    }

    suspend fun restoreMuscuBackupData(backup: MuscuBackupFile) {
        if (backup.version == 1) {
            dao.restoreDatabase(
                sessions = backup.sessions,
                exercises = backup.exercises,
                sets = backup.sets,
                templates = backup.templatesLegacy,
                templateExercises = backup.templateExercises
            )
        } else {
            dao.clearAllData()
            
            backup.workouts.forEach { wDto ->
                val sessionId = dao.insertWorkout(WorkoutSessionEntity(
                    title = wDto.title,
                    date = wDto.date,
                    isLive = wDto.isLive,
                    startTime = wDto.startTime,
                    endTime = wDto.endTime
                ))
                
                wDto.exercises.forEach { eDto ->
                    val exerciseId = dao.insertExercise(ExerciseEntity(
                        sessionId = sessionId,
                        name = eDto.name,
                        sets = eDto.setsCount,
                        reps = eDto.repsCount,
                        weight = eDto.weight,
                        category = eDto.category,
                        note = eDto.note,
                        isFavorite = eDto.isFavorite,
                        isPR = eDto.isPR,
                        order = eDto.order,
                        date = eDto.date
                    ))
                    
                    eDto.detailedSets.forEach { sDto ->
                        dao.insertSet(ExerciseSetEntity(
                            exerciseId = exerciseId,
                            reps = sDto.reps,
                            weight = sDto.weight,
                            isCompleted = sDto.isCompleted,
                            isWarmup = sDto.isWarmup,
                            timestamp = sDto.timestamp,
                            order = sDto.order
                        ))
                    }
                }
            }
            
            backup.templates.forEach { tDto ->
                val templateId = dao.insertTemplate(WorkoutTemplateEntity(name = tDto.name))
                tDto.exercises.forEach { teDto ->
                    dao.insertTemplateExercise(TemplateExerciseEntity(
                        templateId = templateId,
                        name = teDto.name,
                        defaultSets = teDto.defaultSets,
                        defaultReps = teDto.defaultReps,
                        defaultWeight = teDto.defaultWeight,
                        category = teDto.category
                    ))
                }
            }
        }
    }
}

package com.example.muscuapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoSyncTest {
    private lateinit var database: ExerciseDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExerciseDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateSetWithSync_updatesOnlyMatchingExerciseDefinitionOrderAndWarmupState() = runBlocking {
        val exerciseADefinitionId = "Exercise A".toExerciseDefinitionId()
        val differentExerciseDefinitionId = "different-exercise-a".toExerciseDefinitionId()
        val firstSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session A"))
        val secondSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session B"))
        val thirdSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session C"))

        val firstExerciseId = dao.insertExercise(
            ExerciseEntity(
                sessionId = firstSessionId,
                name = "Exercise A",
                sets = 2,
                reps = 10,
                weight = 40f
            )
        )
        val secondExerciseId = dao.insertExercise(
            ExerciseEntity(
                sessionId = secondSessionId,
                name = "Exercise A",
                sets = 2,
                reps = 10,
                weight = 40f
            )
        )
        val sameNameDifferentDefinitionId = dao.insertExercise(
            ExerciseEntity(
                sessionId = thirdSessionId,
                name = "Exercise A",
                exerciseDefinitionId = differentExerciseDefinitionId,
                sets = 2,
                reps = 10,
                weight = 40f
            )
        )

        dao.insertSet(ExerciseSetEntity(exerciseId = firstExerciseId, reps = 10, weight = 40f, order = 0))
        val targetSetId = dao.insertSet(ExerciseSetEntity(exerciseId = firstExerciseId, reps = 10, weight = 45f, order = 1))
        dao.insertSet(ExerciseSetEntity(exerciseId = firstExerciseId, reps = 12, weight = 20f, isWarmup = true, order = 1))

        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 10, weight = 40f, order = 0))
        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 10, weight = 45f, order = 1))
        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 12, weight = 20f, isWarmup = true, order = 1))

        dao.insertSet(ExerciseSetEntity(exerciseId = sameNameDifferentDefinitionId, reps = 10, weight = 40f, order = 0))
        dao.insertSet(ExerciseSetEntity(exerciseId = sameNameDifferentDefinitionId, reps = 10, weight = 45f, order = 1))

        dao.updateSetWithSync(
            ExerciseSetEntity(
                setId = targetSetId,
                exerciseId = firstExerciseId,
                reps = 8,
                weight = 55f,
                order = 1
            )
        )

        val updatedExercises = dao.getAllWorkoutsOnce().flatMap { it.exercises }

        val matchingDefinitionSets = updatedExercises
            .filter { it.exercise.exerciseDefinitionId == exerciseADefinitionId }
            .flatMap { it.sets }
        val differentDefinitionSets = updatedExercises
            .single { it.exercise.exerciseDefinitionId == differentExerciseDefinitionId }
            .sets

        val regularOrderZeroSets = matchingDefinitionSets.filter { !it.isWarmup && it.order == 0 }
        val regularOrderOneSets = matchingDefinitionSets.filter { !it.isWarmup && it.order == 1 }
        val warmupOrderOneSets = matchingDefinitionSets.filter { it.isWarmup && it.order == 1 }

        assertEquals(2, regularOrderZeroSets.size)
        regularOrderZeroSets.forEach { set ->
            assertEquals(40f, set.weight, 0.001f)
            assertEquals(10, set.reps)
        }

        assertEquals(2, regularOrderOneSets.size)
        regularOrderOneSets.forEach { set ->
            assertEquals(55f, set.weight, 0.001f)
            assertEquals(8, set.reps)
        }

        assertEquals(2, warmupOrderOneSets.size)
        warmupOrderOneSets.forEach { set ->
            assertEquals(20f, set.weight, 0.001f)
            assertEquals(12, set.reps)
        }

        val differentDefinitionOrderOne = differentDefinitionSets.single { !it.isWarmup && it.order == 1 }
        assertEquals(45f, differentDefinitionOrderOne.weight, 0.001f)
        assertEquals(10, differentDefinitionOrderOne.reps)

        updatedExercises
            .filter { it.exercise.exerciseDefinitionId == exerciseADefinitionId }
            .forEach { exerciseWithSets ->
                assertEquals(55f, exerciseWithSets.exercise.weight, 0.001f)
                assertEquals(8, exerciseWithSets.exercise.reps)
                assertEquals(2, exerciseWithSets.exercise.sets)
            }
    }

    @Test
    fun updateExerciseWithSync_keepsTemplatesAndOtherNotesIndependent() = runBlocking {
        val exerciseADefinitionId = "Exercise A".toExerciseDefinitionId()
        val firstSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session A"))
        val secondSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session B"))
        val templateId = dao.insertTemplate(WorkoutTemplateEntity(name = "Template A"))

        val firstExerciseId = dao.insertExercise(
            ExerciseEntity(
                sessionId = firstSessionId,
                name = "Exercise A",
                sets = 2,
                reps = 10,
                weight = 40f,
                category = "Autre",
                note = "old local note"
            )
        )
        dao.insertExercise(
            ExerciseEntity(
                sessionId = secondSessionId,
                name = "Exercise A",
                sets = 2,
                reps = 10,
                weight = 40f,
                category = "Autre",
                note = "other session note"
            )
        )
        dao.insertTemplateExercise(
            TemplateExerciseEntity(
                templateId = templateId,
                name = "Exercise A",
                exerciseDefinitionId = exerciseADefinitionId,
                defaultSets = 2,
                defaultReps = 10,
                defaultWeight = 40f,
                category = "Autre"
            )
        )

        dao.updateExerciseWithSync(
            ExerciseEntity(
                id = firstExerciseId,
                sessionId = firstSessionId,
                name = "Exercise A renamed",
                exerciseDefinitionId = exerciseADefinitionId,
                sets = 3,
                reps = 8,
                weight = 60f,
                category = "Pectoraux",
                note = "new local note"
            )
        )

        val updatedExercises = dao.getAllWorkoutsOnce().flatMap { it.exercises }
        assertEquals(2, updatedExercises.size)
        val firstExercise = updatedExercises.single { it.exercise.id == firstExerciseId }.exercise
        val otherExercise = updatedExercises.single { it.exercise.id != firstExerciseId }.exercise
        assertEquals("Exercise A renamed", firstExercise.name)
        assertEquals(3, firstExercise.sets)
        assertEquals(8, firstExercise.reps)
        assertEquals(60f, firstExercise.weight, 0.001f)
        assertEquals("Pectoraux", firstExercise.category)
        assertEquals("new local note", firstExercise.note)

        assertEquals("Exercise A renamed", otherExercise.name)
        assertEquals(2, otherExercise.sets)
        assertEquals(10, otherExercise.reps)
        assertEquals(40f, otherExercise.weight, 0.001f)
        assertEquals("Pectoraux", otherExercise.category)
        assertEquals("other session note", otherExercise.note)

        val templateExercise = dao.getAllTemplatesOnce().single().exercises.single()
        assertEquals("Exercise A", templateExercise.name)
        assertEquals(2, templateExercise.defaultSets)
        assertEquals(10, templateExercise.defaultReps)
        assertEquals(40f, templateExercise.defaultWeight, 0.001f)
        assertEquals("Autre", templateExercise.category)
    }

    @Test
    fun replaceExerciseSetsWithSync_updatesPersistedMatchingSets() = runBlocking {
        val firstSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session A"))
        val secondSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session B"))
        val firstExerciseId = dao.insertExercise(
            ExerciseEntity(
                sessionId = firstSessionId,
                name = "Exercise A",
                sets = 2,
                reps = 10,
                weight = 45f
            )
        )
        val secondExerciseId = dao.insertExercise(
            ExerciseEntity(
                sessionId = secondSessionId,
                name = "Exercise A",
                sets = 2,
                reps = 10,
                weight = 45f
            )
        )

        val firstOrderZeroId = dao.insertSet(
            ExerciseSetEntity(exerciseId = firstExerciseId, reps = 10, weight = 40f, order = 0)
        )
        val firstOrderOneId = dao.insertSet(
            ExerciseSetEntity(exerciseId = firstExerciseId, reps = 10, weight = 45f, order = 1)
        )
        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 10, weight = 40f, order = 0))
        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 10, weight = 45f, order = 1))

        dao.replaceExerciseSetsWithSync(
            exerciseId = firstExerciseId,
            desiredSets = listOf(
                ExerciseSetEntity(
                    setId = firstOrderZeroId,
                    exerciseId = firstExerciseId,
                    reps = 10,
                    weight = 40f,
                    order = 0
                ),
                ExerciseSetEntity(
                    setId = firstOrderOneId,
                    exerciseId = firstExerciseId,
                    reps = 8,
                    weight = 60f,
                    order = 1
                )
            )
        )

        val updatedExercises = dao.getAllWorkoutsOnce().flatMap { it.exercises }
        updatedExercises.forEach { exerciseWithSets ->
            assertEquals(2, exerciseWithSets.sets.size)
            val orderZero = exerciseWithSets.sets.single { it.order == 0 }
            val orderOne = exerciseWithSets.sets.single { it.order == 1 }
            assertEquals(40f, orderZero.weight, 0.001f)
            assertEquals(10, orderZero.reps)
            assertEquals(60f, orderOne.weight, 0.001f)
            assertEquals(8, orderOne.reps)
            assertEquals(60f, exerciseWithSets.exercise.weight, 0.001f)
            assertEquals(8, exerciseWithSets.exercise.reps)
            assertEquals(2, exerciseWithSets.exercise.sets)
        }
    }
}

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
                exerciseDefinitionId = "different-exercise-a",
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
            .filter { it.exercise.exerciseDefinitionId == "Exercise A" }
            .flatMap { it.sets }
        val differentDefinitionSets = updatedExercises
            .single { it.exercise.exerciseDefinitionId == "different-exercise-a" }
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
            .filter { it.exercise.exerciseDefinitionId == "Exercise A" }
            .forEach { exerciseWithSets ->
                assertEquals(55f, exerciseWithSets.exercise.weight, 0.001f)
                assertEquals(8, exerciseWithSets.exercise.reps)
                assertEquals(2, exerciseWithSets.exercise.sets)
            }
    }
}

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
    fun updateSetWithSync_updatesOnlyMatchingExerciseNameOrderAndWarmupState() = runBlocking {
        val firstSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session A"))
        val secondSessionId = dao.insertWorkout(WorkoutSessionEntity(title = "Session B"))

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

        dao.insertSet(ExerciseSetEntity(exerciseId = firstExerciseId, reps = 10, weight = 40f, order = 0))
        val targetSetId = dao.insertSet(ExerciseSetEntity(exerciseId = firstExerciseId, reps = 10, weight = 45f, order = 1))
        dao.insertSet(ExerciseSetEntity(exerciseId = firstExerciseId, reps = 12, weight = 20f, isWarmup = true, order = 1))

        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 10, weight = 40f, order = 0))
        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 10, weight = 45f, order = 1))
        dao.insertSet(ExerciseSetEntity(exerciseId = secondExerciseId, reps = 12, weight = 20f, isWarmup = true, order = 1))

        dao.updateSetWithSync(
            ExerciseSetEntity(
                setId = targetSetId,
                exerciseId = firstExerciseId,
                reps = 8,
                weight = 55f,
                order = 1
            )
        )

        val updatedSets = dao.getAllWorkoutsOnce()
            .flatMap { it.exercises }
            .flatMap { it.sets }

        val regularOrderZeroSets = updatedSets.filter { !it.isWarmup && it.order == 0 }
        val regularOrderOneSets = updatedSets.filter { !it.isWarmup && it.order == 1 }
        val warmupOrderOneSets = updatedSets.filter { it.isWarmup && it.order == 1 }

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
    }
}

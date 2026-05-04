package com.example.muscuapp.util

import com.example.muscuapp.data.local.WorkoutSessionEntity
import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class WorkoutSuggestionTest {

    @Test
    fun `prefers the workout whose title matches today's split`() {
        val workouts = listOf(
            workout("Legs Day"),
            workout("Push Day"),
            workout("Pull Day")
        )

        val suggestion = suggestWorkoutForToday(workouts, Calendar.MONDAY)

        assertNotNull(suggestion)
        assertEquals("Push Day", suggestion?.workout?.session?.title)
    }

    @Test
    fun `falls back to weekday order when nothing matches the title`() {
        val workouts = listOf(
            workout("Séance A"),
            workout("Séance B"),
            workout("Séance C")
        )

        val suggestion = suggestWorkoutForToday(workouts, Calendar.WEDNESDAY)

        assertNotNull(suggestion)
        assertEquals("Séance C", suggestion?.workout?.session?.title)
    }

    private fun workout(title: String): WorkoutWithExercisesAndSets {
        return WorkoutWithExercisesAndSets(
            session = WorkoutSessionEntity(title = title),
            exercises = emptyList()
        )
    }
}
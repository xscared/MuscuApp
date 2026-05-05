package com.example.muscuapp.util

import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class WorkoutSuggestion(
    val workout: WorkoutWithExercisesAndSets,
    val reason: String
)

fun suggestWorkoutForToday(
    workouts: List<WorkoutWithExercisesAndSets>,
    dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
    manualSchedule: Map<Int, Long?> = emptyMap()
): WorkoutSuggestion? {
    if (workouts.isEmpty()) return null

    manualSchedule[dayOfWeek]?.let { scheduledWorkoutId ->
        workouts.firstOrNull { it.session.sessionId == scheduledWorkoutId }?.let { workout ->
            return WorkoutSuggestion(
                workout = workout,
                reason = "Séance définie dans tes paramètres pour ${dayOfWeekLabel(dayOfWeek).lowercase(Locale.getDefault())}."
            )
        }
    }

    val dayRule = dayRules[dayOfWeek] ?: dayRules[Calendar.MONDAY]!!

    return workouts
        .mapIndexed { index, workout ->
            val title = normalizeText(workout.session.title)
            val primaryMatch = dayRule.primaryKeywords.firstOrNull { title.contains(it) }
            val secondaryMatch = dayRule.secondaryKeywords.firstOrNull { title.contains(it) }
            val fallbackScore = workouts.size - abs(index - dayRule.fallbackIndex)

            val (score, reason) = when {
                primaryMatch != null -> 100 + (dayRule.primaryKeywords.size - dayRule.primaryKeywords.indexOf(primaryMatch)) to
                    "Correspond au ${dayRule.label.lowercase(Locale.getDefault())}."

                secondaryMatch != null -> 60 + (dayRule.secondaryKeywords.size - dayRule.secondaryKeywords.indexOf(secondaryMatch)) to
                    "Correspond au split du ${dayRule.label.lowercase(Locale.getDefault())}."

                else -> fallbackScore to "Séance suggérée selon le jour et l'ordre de tes séances."
            }

            RankedWorkout(workout = workout, score = score, reason = reason)
        }
        .maxByOrNull { it.score }
        ?.let { WorkoutSuggestion(it.workout, it.reason) }
}

fun orderedDayOfWeekValues(): List<Int> = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY
)

fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.MONDAY -> "Lundi"
    Calendar.TUESDAY -> "Mardi"
    Calendar.WEDNESDAY -> "Mercredi"
    Calendar.THURSDAY -> "Jeudi"
    Calendar.FRIDAY -> "Vendredi"
    Calendar.SATURDAY -> "Samedi"
    Calendar.SUNDAY -> "Dimanche"
    else -> "Jour"
}

private data class DayRule(
    val label: String,
    val fallbackIndex: Int,
    val primaryKeywords: List<String>,
    val secondaryKeywords: List<String>
)

private data class RankedWorkout(
    val workout: WorkoutWithExercisesAndSets,
    val score: Int,
    val reason: String
)

private val dayRules = mapOf(
    Calendar.MONDAY to DayRule(
        label = "Lundi",
        fallbackIndex = 0,
        primaryKeywords = listOf("lundi", "lun", "monday", "mon"),
        secondaryKeywords = listOf("push", "pector", "pec", "chest")
    ),
    Calendar.TUESDAY to DayRule(
        label = "Mardi",
        fallbackIndex = 1,
        primaryKeywords = listOf("mardi", "mar", "tuesday", "tue"),
        secondaryKeywords = listOf("pull", "dos", "back", "tirage")
    ),
    Calendar.WEDNESDAY to DayRule(
        label = "Mercredi",
        fallbackIndex = 2,
        primaryKeywords = listOf("mercredi", "mer", "wednesday", "wed"),
        secondaryKeywords = listOf("legs", "jambes", "lower", "squat")
    ),
    Calendar.THURSDAY to DayRule(
        label = "Jeudi",
        fallbackIndex = 3,
        primaryKeywords = listOf("jeudi", "jeu", "thursday", "thu"),
        secondaryKeywords = listOf("upper", "haut", "shoulder", "épaul", "epaul")
    ),
    Calendar.FRIDAY to DayRule(
        label = "Vendredi",
        fallbackIndex = 4,
        primaryKeywords = listOf("vendredi", "ven", "friday", "fri"),
        secondaryKeywords = listOf("arms", "bras", "biceps", "triceps")
    ),
    Calendar.SATURDAY to DayRule(
        label = "Samedi",
        fallbackIndex = 5,
        primaryKeywords = listOf("samedi", "sam", "saturday", "sat"),
        secondaryKeywords = listOf("full body", "fullbody", "cardio", "conditioning")
    ),
    Calendar.SUNDAY to DayRule(
        label = "Dimanche",
        fallbackIndex = 6,
        primaryKeywords = listOf("dimanche", "dim", "sunday", "sun"),
        secondaryKeywords = listOf("repos", "rest", "recovery", "mobility")
    )
)

private fun normalizeText(text: String): String {
    val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
    return normalized
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.getDefault())
}
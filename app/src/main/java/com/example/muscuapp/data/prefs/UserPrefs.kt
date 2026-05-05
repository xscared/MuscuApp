package com.example.muscuapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

enum class WeightUnit { KG, LBS }
enum class ThemeMode { LIGHT, DARK, SYSTEM }

class UserPrefs(private val context: Context) {
    private val UNIT_KEY = stringPreferencesKey("weight_unit")
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val HAPTIC_ENABLED_KEY = booleanPreferencesKey("haptic_enabled")
    private val HAPTIC_INTENSITY_KEY = floatPreferencesKey("haptic_intensity")
    private val ALARM_SOUND_KEY = stringPreferencesKey("alarm_sound")
    private val preferredWorkoutKeys = mapOf(
        Calendar.MONDAY to longPreferencesKey("preferred_workout_monday"),
        Calendar.TUESDAY to longPreferencesKey("preferred_workout_tuesday"),
        Calendar.WEDNESDAY to longPreferencesKey("preferred_workout_wednesday"),
        Calendar.THURSDAY to longPreferencesKey("preferred_workout_thursday"),
        Calendar.FRIDAY to longPreferencesKey("preferred_workout_friday"),
        Calendar.SATURDAY to longPreferencesKey("preferred_workout_saturday"),
        Calendar.SUNDAY to longPreferencesKey("preferred_workout_sunday")
    )

    val weightUnit: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
        WeightUnit.valueOf(prefs[UNIT_KEY] ?: WeightUnit.KG.name)
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.valueOf(prefs[THEME_KEY] ?: ThemeMode.SYSTEM.name)
    }

    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAPTIC_ENABLED_KEY] ?: true
    }

    val hapticIntensity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[HAPTIC_INTENSITY_KEY] ?: 1.0f
    }

    val alarmSound: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ALARM_SOUND_KEY]
    }

    val preferredWorkoutIdsByDay: Flow<Map<Int, Long?>> = context.dataStore.data.map { prefs ->
        preferredWorkoutKeys.mapValues { (_, key) -> prefs[key] }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { prefs ->
            prefs[UNIT_KEY] = unit.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAPTIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setHapticIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[HAPTIC_INTENSITY_KEY] = intensity
        }
    }

    suspend fun setAlarmSound(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[ALARM_SOUND_KEY] = uri
            else prefs.remove(ALARM_SOUND_KEY)
        }
    }

    suspend fun setPreferredWorkoutForDay(dayOfWeek: Int, workoutId: Long?) {
        val key = preferredWorkoutKeys[dayOfWeek] ?: return

        context.dataStore.edit { prefs ->
            if (workoutId != null) {
                prefs[key] = workoutId
            } else {
                prefs.remove(key)
            }
        }
    }
}

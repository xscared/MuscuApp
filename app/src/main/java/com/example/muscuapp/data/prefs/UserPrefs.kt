package com.example.muscuapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

enum class WeightUnit { KG, LBS }
enum class ThemeMode { LIGHT, DARK, SYSTEM }

class UserPrefs(private val context: Context) {
    private val UNIT_KEY = stringPreferencesKey("weight_unit")
    private val THEME_KEY = stringPreferencesKey("theme_mode")

    val weightUnit: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
        WeightUnit.valueOf(prefs[UNIT_KEY] ?: WeightUnit.KG.name)
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.valueOf(prefs[THEME_KEY] ?: ThemeMode.SYSTEM.name)
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
}

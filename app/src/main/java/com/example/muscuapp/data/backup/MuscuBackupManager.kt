package com.example.muscuapp.data.backup

import android.content.Context
import android.net.Uri
import com.example.muscuapp.data.prefs.UserPrefs
import com.example.muscuapp.data.prefs.WeightUnit
import com.example.muscuapp.data.repository.ExerciseRepository
import com.example.muscuapp.util.orderedDayOfWeekValues
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MuscuBackupManager @Inject constructor(
    private val repository: ExerciseRepository,
    private val userPrefs: UserPrefs,
    @ApplicationContext private val context: Context
) {
    suspend fun createBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = repository.getMuscuBackupData()
            val currentUnit = userPrefs.weightUnit.first()
            val currentSchedule = userPrefs.preferredWorkoutIdsByDay.first()
            val allWorkouts = repository.getAllWorkouts().first()
            
            // Map IDs to Names for a portable backup
            val scheduleNames = currentSchedule.mapValues { (_, id) ->
                allWorkouts.find { it.session.sessionId == id }?.session?.title
            }

            val dataWithPrefs = backupData.copy(
                weightUnit = currentUnit.name,
                preferredWorkoutNamesByDay = scheduleNames
            )
            
            val gson = Gson()
            val jsonString = gson.toJson(dataWithPrefs)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            } ?: throw IllegalStateException("Cannot open output stream for URI")
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val gson = Gson()
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            } ?: throw IllegalStateException("Cannot open input stream for URI")

            val backup = gson.fromJson(jsonString, MuscuBackupFile::class.java)
            
            if (backup.version > 2) {
                throw IllegalArgumentException("Backup version in the future not supported")
            }

            // Restore DB
            repository.restoreMuscuBackupData(backup)
            
            // Restore UserPrefs
            val unitToRestore = try {
                WeightUnit.valueOf(backup.weightUnit)
            } catch (e: Exception) {
                WeightUnit.KG
            }
            userPrefs.setWeightUnit(unitToRestore)
            
            if (backup.version == 1) {
                orderedDayOfWeekValues().forEach { dayOfWeek ->
                    userPrefs.setPreferredWorkoutForDay(dayOfWeek, backup.preferredWorkoutIdsByDay[dayOfWeek])
                }
            } else {
                // Re-map Names to NEW IDs after DB restoration (v2)
                val allNewWorkouts = repository.getAllWorkouts().first()
                orderedDayOfWeekValues().forEach { dayOfWeek ->
                    val workoutName = backup.preferredWorkoutNamesByDay[dayOfWeek]
                    val newId = allNewWorkouts.find { it.session.title == workoutName }?.session?.sessionId
                    userPrefs.setPreferredWorkoutForDay(dayOfWeek, newId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
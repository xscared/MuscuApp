package com.example.muscuapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class, 
        WorkoutSessionEntity::class, 
        WorkoutTemplateEntity::class, 
        TemplateExerciseEntity::class,
        ExerciseSetEntity::class
    ], 
    version = 10,
    exportSchema = false
)
abstract class ExerciseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}

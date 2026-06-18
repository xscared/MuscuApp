package com.example.muscuapp.di

import android.content.Context
import androidx.room.Room
import com.example.muscuapp.data.local.ExerciseDatabase
import com.example.muscuapp.data.local.ExerciseMigrations.MIGRATION_10_11
import com.example.muscuapp.data.local.ExerciseMigrations.MIGRATION_8_9
import com.example.muscuapp.data.local.ExerciseMigrations.MIGRATION_9_10
import com.example.muscuapp.data.repository.ExerciseRepository
import com.example.muscuapp.data.local.ExerciseDao
import com.example.muscuapp.data.prefs.UserPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPrefs(@ApplicationContext context: Context): UserPrefs = UserPrefs(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExerciseDatabase {
        return Room.databaseBuilder(
            context,
            ExerciseDatabase::class.java,
            "exercise_db"
        )
        .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
        .build()
    }

    @Provides
    fun provideExerciseDao(db: ExerciseDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideExerciseRepository(dao: ExerciseDao) = ExerciseRepository(dao)
}

package com.example.muscuapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.muscuapp.data.local.ExerciseDatabase
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
        val migration_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_sets ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
            }
        }

        return Room.databaseBuilder(
            context,
            ExerciseDatabase::class.java,
            "exercise_db"
        )
        .addMigrations(migration_8_9)
        .build()
    }

    @Provides
    fun provideExerciseDao(db: ExerciseDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideExerciseRepository(dao: ExerciseDao) = ExerciseRepository(dao)
}
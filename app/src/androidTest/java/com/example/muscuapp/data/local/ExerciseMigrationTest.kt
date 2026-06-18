package com.example.muscuapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.muscuapp.data.local.ExerciseMigrations.MIGRATION_10_11
import com.example.muscuapp.data.local.ExerciseMigrations.MIGRATION_9_10
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "exercise-migration-test"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration9To11_populatesExerciseDefinitionIds() {
        createDatabase(version = 9, includeDefinitionId = false).use { db ->
            insertVersion9Data(db)
            db.version = 9
        }

        val database = openMigratedDatabase(MIGRATION_9_10, MIGRATION_10_11)
        try {
            assertSingleValue(database, "SELECT exerciseDefinitionId FROM exercises WHERE id = 1", "exercise:exercise a")
            assertSingleValue(database, "SELECT exerciseDefinitionId FROM template_exercises WHERE id = 1", "exercise:exercise a")
        } finally {
            database.close()
        }
    }

    @Test
    fun migration10To11_normalizesRawExerciseDefinitionIds() {
        createDatabase(version = 10, includeDefinitionId = true).use { db ->
            insertVersion10Data(db)
            db.version = 10
        }

        val database = openMigratedDatabase(MIGRATION_10_11)
        try {
            assertSingleValue(database, "SELECT exerciseDefinitionId FROM exercises WHERE id = 1", "exercise:exercise a")
            assertSingleValue(database, "SELECT exerciseDefinitionId FROM template_exercises WHERE id = 1", "exercise:exercise a")
        } finally {
            database.close()
        }
    }

    private fun createDatabase(version: Int, includeDefinitionId: Boolean): SQLiteDatabase {
        context.deleteDatabase(databaseName)
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        createSchema(db, includeDefinitionId)
        db.version = version
        return db
    }

    private fun openMigratedDatabase(vararg migrations: androidx.room.migration.Migration): ExerciseDatabase =
        Room.databaseBuilder(context, ExerciseDatabase::class.java, databaseName)
            .addMigrations(*migrations)
            .allowMainThreadQueries()
            .build()

    private fun assertSingleValue(database: ExerciseDatabase, query: String, expected: String) {
        val cursor = database.openHelper.readableDatabase.query(query)
        try {
            cursor.moveToFirst()
            assertEquals(expected, cursor.getString(0))
        } finally {
            cursor.close()
        }
    }

    private fun createSchema(db: SQLiteDatabase, includeDefinitionId: Boolean) {
        db.execSQL(
            """
            CREATE TABLE workout_sessions (
                sessionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                date INTEGER NOT NULL,
                isLive INTEGER NOT NULL,
                startTime INTEGER,
                endTime INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                name TEXT NOT NULL,
                ${if (includeDefinitionId) "exerciseDefinitionId TEXT NOT NULL," else ""}
                sets INTEGER NOT NULL,
                reps INTEGER NOT NULL,
                weight REAL NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                isPR INTEGER NOT NULL,
                `order` INTEGER NOT NULL,
                date INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(sessionId) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_exercises_sessionId ON exercises(sessionId)")
        if (includeDefinitionId) {
            db.execSQL("CREATE INDEX index_exercises_exerciseDefinitionId ON exercises(exerciseDefinitionId)")
        }

        db.execSQL(
            """
            CREATE TABLE exercise_sets (
                setId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                exerciseId INTEGER NOT NULL,
                reps INTEGER NOT NULL,
                weight REAL NOT NULL,
                isCompleted INTEGER NOT NULL,
                isWarmup INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                `order` INTEGER NOT NULL,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_exercise_sets_exerciseId ON exercise_sets(exerciseId)")

        db.execSQL(
            """
            CREATE TABLE workout_templates (
                templateId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE template_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                templateId INTEGER NOT NULL,
                name TEXT NOT NULL,
                ${if (includeDefinitionId) "exerciseDefinitionId TEXT NOT NULL," else ""}
                defaultSets INTEGER NOT NULL,
                defaultReps INTEGER NOT NULL,
                defaultWeight REAL NOT NULL,
                category TEXT NOT NULL,
                FOREIGN KEY(templateId) REFERENCES workout_templates(templateId) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_template_exercises_templateId ON template_exercises(templateId)")
        if (includeDefinitionId) {
            db.execSQL("CREATE INDEX index_template_exercises_exerciseDefinitionId ON template_exercises(exerciseDefinitionId)")
        }
    }

    private fun insertVersion9Data(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO workout_sessions(sessionId, title, date, isLive, startTime, endTime) VALUES (1, 'Session A', 100, 0, NULL, NULL)")
        db.execSQL(
            """
            INSERT INTO exercises(id, sessionId, name, sets, reps, weight, category, note, isFavorite, isPR, `order`, date)
            VALUES (1, 1, 'Exercise A', 2, 10, 40.0, 'Autre', '', 0, 0, 0, 100)
            """.trimIndent()
        )
        db.execSQL("INSERT INTO workout_templates(templateId, name) VALUES (1, 'Template A')")
        db.execSQL(
            """
            INSERT INTO template_exercises(id, templateId, name, defaultSets, defaultReps, defaultWeight, category)
            VALUES (1, 1, 'Exercise A', 2, 10, 40.0, 'Autre')
            """.trimIndent()
        )
    }

    private fun insertVersion10Data(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO workout_sessions(sessionId, title, date, isLive, startTime, endTime) VALUES (1, 'Session A', 100, 0, NULL, NULL)")
        db.execSQL(
            """
            INSERT INTO exercises(id, sessionId, name, exerciseDefinitionId, sets, reps, weight, category, note, isFavorite, isPR, `order`, date)
            VALUES (1, 1, 'Exercise A', 'Exercise A', 2, 10, 40.0, 'Autre', '', 0, 0, 0, 100)
            """.trimIndent()
        )
        db.execSQL("INSERT INTO workout_templates(templateId, name) VALUES (1, 'Template A')")
        db.execSQL(
            """
            INSERT INTO template_exercises(id, templateId, name, exerciseDefinitionId, defaultSets, defaultReps, defaultWeight, category)
            VALUES (1, 1, 'Exercise A', 'Exercise A', 2, 10, 40.0, 'Autre')
            """.trimIndent()
        )
    }
}

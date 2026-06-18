package com.example.muscuapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ExerciseMigrations {
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exercise_sets ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN exerciseDefinitionId TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                """
                UPDATE exercises
                SET exerciseDefinitionId = CASE
                    WHEN TRIM(name) = '' THEN 'exercise:unnamed-exercise'
                    ELSE 'exercise:' || LOWER(TRIM(name))
                END
                WHERE exerciseDefinitionId = ''
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_exerciseDefinitionId ON exercises(exerciseDefinitionId)")

            db.execSQL("ALTER TABLE template_exercises ADD COLUMN exerciseDefinitionId TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                """
                UPDATE template_exercises
                SET exerciseDefinitionId = CASE
                    WHEN TRIM(name) = '' THEN 'exercise:unnamed-exercise'
                    ELSE 'exercise:' || LOWER(TRIM(name))
                END
                WHERE exerciseDefinitionId = ''
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_exerciseDefinitionId ON template_exercises(exerciseDefinitionId)")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            normalizeExerciseDefinitionIds(db, "exercises")
            normalizeExerciseDefinitionIds(db, "template_exercises")
        }
    }

    private fun normalizeExerciseDefinitionIds(db: SupportSQLiteDatabase, tableName: String) {
        db.execSQL(
            """
            UPDATE $tableName
            SET exerciseDefinitionId = CASE
                WHEN TRIM(exerciseDefinitionId) = '' THEN 'exercise:unnamed-exercise'
                WHEN LOWER(SUBSTR(TRIM(exerciseDefinitionId), 1, 9)) = 'exercise:' THEN LOWER(TRIM(exerciseDefinitionId))
                ELSE 'exercise:' || LOWER(TRIM(exerciseDefinitionId))
            END
            """.trimIndent()
        )
    }
}

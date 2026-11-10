package com.kichikan.negah.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds growth-map storage without rebuilding or clearing any existing table.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS growth_items (
                id TEXT NOT NULL,
                track TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                sortOrder INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS member_growth_progress (
                memberId TEXT NOT NULL,
                growthItemId TEXT NOT NULL,
                progressPercent INTEGER NOT NULL,
                note TEXT,
                updatedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(memberId, growthItemId),
                FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(growthItemId) REFERENCES growth_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_member_growth_progress_growthItemId ON member_growth_progress(growthItemId)",
        )
    }
}

package com.kichikan.negah.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
val MIGRATION_1_2 = object : Migration(1, 2) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS growth_items (id TEXT NOT NULL, track TEXT NOT NULL, title TEXT NOT NULL, description TEXT, sortOrder INTEGER NOT NULL, isActive INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))""")
  database.execSQL("""CREATE TABLE IF NOT EXISTS member_growth_progress (memberId TEXT NOT NULL, growthItemId TEXT NOT NULL, progressPercent INTEGER NOT NULL, note TEXT, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(memberId, growthItemId), FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(growthItemId) REFERENCES growth_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_member_growth_progress_growthItemId ON member_growth_progress(growthItemId)")
 }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS catalog_items (id TEXT NOT NULL, category TEXT NOT NULL, title TEXT NOT NULL, description TEXT, isAvailable INTEGER NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))""")
  database.execSQL("""CREATE TABLE IF NOT EXISTS member_inventory (memberId TEXT NOT NULL, catalogItemId TEXT NOT NULL, acquiredAtEpochMillis INTEGER NOT NULL, sourceType TEXT NOT NULL, sourceId TEXT, PRIMARY KEY(memberId, catalogItemId), FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(catalogItemId) REFERENCES catalog_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_member_inventory_catalogItemId ON member_inventory(catalogItemId)")
  database.execSQL("""CREATE TABLE IF NOT EXISTS member_active_items (memberId TEXT NOT NULL, category TEXT NOT NULL, catalogItemId TEXT NOT NULL, activatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(memberId, category), FOREIGN KEY(memberId, catalogItemId) REFERENCES member_inventory(memberId, catalogItemId) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_member_active_items_memberId_catalogItemId ON member_active_items(memberId, catalogItemId)")
 }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
 override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("ALTER TABLE catalog_items ADD COLUMN priceAmount INTEGER") }
}
val MIGRATION_4_5 = object : Migration(4, 5) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("CREATE TABLE IF NOT EXISTS class_days (weekdayIndex INTEGER NOT NULL, PRIMARY KEY(weekdayIndex))")
 }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("ALTER TABLE sessions ADD COLUMN topic TEXT")
  database.execSQL("ALTER TABLE sessions ADD COLUMN location TEXT")
  database.execSQL("ALTER TABLE sessions ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
  database.execSQL("""CREATE TABLE IF NOT EXISTS attendance_revisions (id TEXT NOT NULL, attendanceRecordId TEXT NOT NULL, sessionId TEXT NOT NULL, memberId TEXT NOT NULL, previousStatus TEXT NOT NULL, previousReason TEXT, previousIsConfirmed INTEGER NOT NULL, previousUpdatedAtEpochMillis INTEGER NOT NULL, changedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(attendanceRecordId) REFERENCES attendance_records(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_revisions_attendanceRecordId ON attendance_revisions(attendanceRecordId)")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_revisions_sessionId ON attendance_revisions(sessionId)")
 }
}

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
val MIGRATION_6_7 = object : Migration(6, 7) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("CREATE TABLE IF NOT EXISTS home_shortcut_selections (shortcutId TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(shortcutId))")
  database.execSQL("CREATE TABLE IF NOT EXISTS home_shortcut_settings (id INTEGER NOT NULL, configured INTEGER NOT NULL, PRIMARY KEY(id))")
  database.execSQL("INSERT INTO home_shortcut_settings (id, configured) VALUES (1, 0)")
 }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
 override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("ALTER TABLE members ADD COLUMN birthDateJalali TEXT") }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS media_assets (id TEXT NOT NULL, ownerType TEXT NOT NULL, ownerId TEXT NOT NULL, role TEXT NOT NULL, relativePath TEXT NOT NULL, mimeType TEXT NOT NULL, byteSize INTEGER NOT NULL, sha256 TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))""")
  database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_media_assets_ownerType_ownerId_role ON media_assets(ownerType, ownerId, role)")
 }
}
val MIGRATION_9_10 = object : Migration(9, 10) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS ring_profile (id TEXT NOT NULL, name TEXT NOT NULL, username TEXT, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))""")
 }
}
val MIGRATION_10_11 = object : Migration(10, 11) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS missions (id TEXT NOT NULL, kind TEXT NOT NULL, title TEXT NOT NULL, description TEXT, rewardCurrency TEXT NOT NULL, rewardAmount INTEGER NOT NULL, isActive INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))""")
  database.execSQL("""CREATE TABLE IF NOT EXISTS mission_assignments (id TEXT NOT NULL, missionId TEXT NOT NULL, memberId TEXT NOT NULL, status TEXT NOT NULL, coachProgressNote TEXT, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(missionId) REFERENCES missions(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_mission_assignments_missionId ON mission_assignments(missionId)")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_mission_assignments_memberId ON mission_assignments(memberId)")
  database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_mission_assignments_missionId_memberId ON mission_assignments(missionId, memberId)")
 }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS daily_quotes (id TEXT NOT NULL, text TEXT NOT NULL, isActive INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))""")
 }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("ALTER TABLE members ADD COLUMN activeBadgeLevel INTEGER")
 }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS book_readings (id TEXT NOT NULL, memberId TEXT NOT NULL, title TEXT NOT NULL, readAtEpochMillis INTEGER NOT NULL, level INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_book_readings_memberId ON book_readings(memberId)")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_book_readings_readAtEpochMillis ON book_readings(readAtEpochMillis)")
  database.execSQL("""CREATE TABLE IF NOT EXISTS member_growth_history (id TEXT NOT NULL, memberId TEXT NOT NULL, growthItemId TEXT NOT NULL, previousPercent INTEGER, newPercent INTEGER NOT NULL, note TEXT, changedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(growthItemId) REFERENCES growth_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_member_growth_history_memberId ON member_growth_history(memberId)")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_member_growth_history_growthItemId ON member_growth_history(growthItemId)")
 }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
 override fun migrate(database: SupportSQLiteDatabase) {
  database.execSQL("""CREATE TABLE IF NOT EXISTS member_groups (id TEXT NOT NULL, name TEXT NOT NULL, leaderMemberId TEXT NOT NULL, isActive INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(leaderMemberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_member_groups_leaderMemberId ON member_groups(leaderMemberId)")
  database.execSQL("""CREATE TABLE IF NOT EXISTS group_members (groupId TEXT NOT NULL, memberId TEXT NOT NULL, PRIMARY KEY(groupId, memberId), FOREIGN KEY(groupId) REFERENCES member_groups(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(memberId) REFERENCES members(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
  database.execSQL("CREATE INDEX IF NOT EXISTS index_group_members_memberId ON group_members(memberId)")
 }
}

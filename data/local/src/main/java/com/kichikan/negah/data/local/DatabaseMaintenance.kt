package com.kichikan.negah.data.local

import javax.inject.Inject

/** Room-free handle for the app module: flush the WAL before copying the database file. */
class DatabaseMaintenance @Inject constructor(private val database: NegahDatabase) {
    fun checkpoint() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    fun schemaVersion(): Int = database.openHelper.readableDatabase.version
}

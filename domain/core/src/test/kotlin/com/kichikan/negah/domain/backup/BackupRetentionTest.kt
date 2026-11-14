package com.kichikan.negah.domain.backup

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRetentionTest {
    @Test
    fun nameRoundTripsAndIsRecognised() {
        val name = BackupRetention.fileName(LocalDateTime.of(2026, 11, 14, 22, 5, 9))
        assertEquals("negah-backup-20261114-220509.zip", name)
        assertTrue(BackupRetention.isBackupName(name))
        assertFalse(BackupRetention.isBackupName("notes.zip"))
        assertFalse(BackupRetention.isBackupName("negah-backup-abc.zip"))
    }

    @Test
    fun keepsNewestTwoAndNeverTouchesOtherFiles() {
        val names = listOf(
            "negah-backup-20261101-100000.zip", "negah-backup-20261103-100000.zip",
            "photo.jpg", "negah-backup-20261102-100000.zip", "negah-backup-20261104-100000.zip",
        )
        assertEquals(
            listOf("negah-backup-20261102-100000.zip", "negah-backup-20261101-100000.zip"),
            BackupRetention.namesToDelete(names),
        )
        assertTrue(BackupRetention.namesToDelete(names.take(2)).isEmpty())
    }
}

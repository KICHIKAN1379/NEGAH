package com.kichikan.negah.domain.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Naming and retention rules for local backups (owner decision: at most two versions). */
object BackupRetention {
    const val MAX_VERSIONS = 2
    private const val PREFIX = "negah-backup-"
    private const val SUFFIX = ".zip"
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun fileName(at: LocalDateTime): String = PREFIX + stamp.format(at) + SUFFIX

    fun isBackupName(name: String): Boolean =
        name.startsWith(PREFIX) && name.endsWith(SUFFIX) &&
            runCatching { LocalDateTime.parse(name.removePrefix(PREFIX).removeSuffix(SUFFIX), stamp) }.isSuccess

    /** Names to delete so only the newest [keep] backups remain; other files are never touched. */
    fun namesToDelete(existing: List<String>, keep: Int = MAX_VERSIONS): List<String> =
        existing.filter(::isBackupName).sortedDescending().drop(keep.coerceAtLeast(1))
}

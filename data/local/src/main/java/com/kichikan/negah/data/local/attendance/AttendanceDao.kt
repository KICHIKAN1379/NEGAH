package com.kichikan.negah.data.local.attendance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY memberId")
    abstract fun observeForSession(sessionId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records")
    abstract fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_revisions WHERE sessionId = :sessionId ORDER BY changedAtEpochMillis DESC")
    abstract fun observeRevisionsForSession(sessionId: String): Flow<List<AttendanceRevisionEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND memberId = :memberId LIMIT 1")
    protected abstract suspend fun findBySessionAndMember(
        sessionId: String,
        memberId: String,
    ): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertRevisions(revisions: List<AttendanceRevisionEntity>)

    @Upsert
    protected abstract suspend fun upsertAll(records: List<AttendanceEntity>)

    @Transaction
    open suspend fun saveForSession(records: List<AttendanceEntity>) {
        if (records.isEmpty()) return

        val revisions = mutableListOf<AttendanceRevisionEntity>()
        val normalizedRecords = records.map { incoming ->
            val previous = findBySessionAndMember(incoming.sessionId, incoming.memberId)
            if (previous == null) {
                incoming
            } else {
                val changed = previous.status != incoming.status ||
                    previous.reason != incoming.reason ||
                    previous.isConfirmed != incoming.isConfirmed
                if (changed) {
                    revisions += AttendanceRevisionEntity(
                        id = UUID.randomUUID().toString(),
                        attendanceRecordId = previous.id,
                        sessionId = previous.sessionId,
                        memberId = previous.memberId,
                        previousStatus = previous.status.name,
                        previousReason = previous.reason,
                        previousIsConfirmed = previous.isConfirmed,
                        previousUpdatedAtEpochMillis = previous.updatedAtEpochMillis,
                        changedAtEpochMillis = incoming.updatedAtEpochMillis,
                    )
                }
                incoming.copy(id = previous.id)
            }
        }

        if (revisions.isNotEmpty()) insertRevisions(revisions)
        upsertAll(normalizedRecords)
    }
}

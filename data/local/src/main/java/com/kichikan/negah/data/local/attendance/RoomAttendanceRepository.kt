package com.kichikan.negah.data.local.attendance

import com.kichikan.negah.domain.attendance.AttendanceRecord
import com.kichikan.negah.domain.attendance.AttendanceRepository
import com.kichikan.negah.domain.attendance.AttendanceRevision
import com.kichikan.negah.domain.attendance.AttendanceStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAttendanceRepository @Inject constructor(
    private val dao: AttendanceDao,
) : AttendanceRepository {
    override fun observeForSession(sessionId: String): Flow<List<AttendanceRecord>> =
        dao.observeForSession(sessionId).map { records -> records.map(AttendanceEntity::toDomain) }

    override fun observeAll(): Flow<List<AttendanceRecord>> =
        dao.observeAll().map { records -> records.map(AttendanceEntity::toDomain) }

    override fun observeRevisionsForSession(sessionId: String): Flow<List<AttendanceRevision>> =
        dao.observeRevisionsForSession(sessionId).map { revisions ->
            revisions.map { revision ->
                AttendanceRevision(
                    id = revision.id,
                    attendanceRecordId = revision.attendanceRecordId,
                    sessionId = revision.sessionId,
                    memberId = revision.memberId,
                    previousStatus = AttendanceStatus.valueOf(revision.previousStatus),
                    previousReason = revision.previousReason,
                    previousIsConfirmed = revision.previousIsConfirmed,
                    previousUpdatedAtEpochMillis = revision.previousUpdatedAtEpochMillis,
                    changedAtEpochMillis = revision.changedAtEpochMillis,
                )
            }
        }

    override suspend fun saveForSession(records: List<AttendanceRecord>) {
        dao.saveForSession(records.map { record ->
            AttendanceEntity(
                id = record.id,
                sessionId = record.sessionId,
                memberId = record.memberId,
                status = record.status,
                reason = record.reason,
                isConfirmed = record.isConfirmed,
                updatedAtEpochMillis = record.updatedAtEpochMillis,
            )
        })
    }
}

private fun AttendanceEntity.toDomain() = AttendanceRecord(
    id = id,
    sessionId = sessionId,
    memberId = memberId,
    status = status,
    reason = reason,
    isConfirmed = isConfirmed,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

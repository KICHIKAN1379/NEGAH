package com.kichikan.negah.domain.attendance

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

data class AttendanceDraft(
    val memberId: String,
    val status: AttendanceStatus,
    val reason: String?,
)

data class AttendanceRecord(
    val id: String,
    val sessionId: String,
    val memberId: String,
    val status: AttendanceStatus,
    val reason: String?,
    val isConfirmed: Boolean,
    val updatedAtEpochMillis: Long,
)

data class AttendanceRevision(
    val id: String,
    val attendanceRecordId: String,
    val sessionId: String,
    val memberId: String,
    val previousStatus: AttendanceStatus,
    val previousReason: String?,
    val previousIsConfirmed: Boolean,
    val previousUpdatedAtEpochMillis: Long,
    val changedAtEpochMillis: Long,
)

interface AttendanceRepository {
    fun observeForSession(sessionId: String): Flow<List<AttendanceRecord>>
    fun observeAll(): Flow<List<AttendanceRecord>>
    fun observeRevisionsForSession(sessionId: String): Flow<List<AttendanceRevision>>
    suspend fun saveForSession(records: List<AttendanceRecord>)
}

sealed interface SaveAttendanceResult {
    data object Saved : SaveAttendanceResult
    data class ReasonRequired(val memberId: String) : SaveAttendanceResult
    data object DuplicateMember : SaveAttendanceResult
}

class SaveAttendance @Inject constructor(
    private val repository: AttendanceRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        drafts: List<AttendanceDraft>,
        nowEpochMillis: Long,
    ): SaveAttendanceResult {
        if (drafts.map { it.memberId }.distinct().size != drafts.size) {
            return SaveAttendanceResult.DuplicateMember
        }

        val invalid = drafts.firstOrNull {
            AttendanceRules.validate(it.status, it.reason) == AttendanceValidation.ReasonRequired
        }
        if (invalid != null) return SaveAttendanceResult.ReasonRequired(invalid.memberId)

        val records = drafts.map { draft ->
            val stableId = UUID.nameUUIDFromBytes(
                (sessionId + ":" + draft.memberId).toByteArray(Charsets.UTF_8),
            ).toString()
            AttendanceRecord(
                id = stableId,
                sessionId = sessionId,
                memberId = draft.memberId,
                status = draft.status,
                reason = draft.reason?.trim()?.takeIf(String::isNotBlank),
                isConfirmed = true,
                updatedAtEpochMillis = nowEpochMillis,
            )
        }
        repository.saveForSession(records)
        return SaveAttendanceResult.Saved
    }
}

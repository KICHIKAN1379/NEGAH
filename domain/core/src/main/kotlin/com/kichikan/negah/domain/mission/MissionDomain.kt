package com.kichikan.negah.domain.mission

import com.kichikan.negah.domain.economy.EconomyCurrency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

enum class MissionKind { REGULAR, HIDDEN, GROUP_PUZZLE }
enum class MissionAssignmentStatus { ASSIGNED, IN_PROGRESS, COMPLETED, APPROVED }

data class Mission(
    val id: String,
    val kind: MissionKind,
    val title: String,
    val description: String?,
    val rewardCurrency: EconomyCurrency,
    val rewardAmount: Long,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
)

data class MissionAssignment(
    val id: String,
    val missionId: String,
    val memberId: String,
    val status: MissionAssignmentStatus,
    val coachProgressNote: String?,
    val updatedAtEpochMillis: Long,
)

interface MissionRepository {
    fun observeMissions(): Flow<List<Mission>>
    fun observeAssignments(missionId: String): Flow<List<MissionAssignment>>
    suspend fun saveMission(mission: Mission)
    suspend fun assignMember(assignment: MissionAssignment)
    suspend fun updateProgress(assignmentId: String, note: String?, completed: Boolean, updatedAt: Long)
    /** Returns true only when a new reward ledger row was appended. */
    suspend fun approveAndReward(assignmentId: String, approvedAt: Long): Boolean
}

class ApproveMissionCompletion @Inject constructor(
    private val repository: MissionRepository,
) {
    suspend operator fun invoke(assignmentId: String, approvedAt: Long): Boolean =
        assignmentId.isNotBlank() && repository.approveAndReward(assignmentId, approvedAt)
}

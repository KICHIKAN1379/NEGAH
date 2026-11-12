package com.kichikan.negah.data.local.mission

import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.mission.Mission
import com.kichikan.negah.domain.mission.MissionAssignment
import com.kichikan.negah.domain.mission.MissionAssignmentStatus
import com.kichikan.negah.domain.mission.MissionKind
import com.kichikan.negah.domain.mission.MissionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMissionRepository @Inject constructor(
    private val dao: MissionDao,
) : MissionRepository {
    override fun observeMissions(): Flow<List<Mission>> =
        dao.observeMissions().map { rows -> rows.map(MissionEntity::toDomain) }

    override fun observeAssignments(missionId: String): Flow<List<MissionAssignment>> =
        dao.observeAssignments(missionId).map { rows -> rows.map(MissionAssignmentEntity::toDomain) }

    override suspend fun saveMission(mission: Mission) {
        require(mission.id.isNotBlank() && mission.title.isNotBlank())
        require(mission.rewardAmount > 0L)
        dao.saveMission(mission.toEntity())
    }

    override suspend fun assignMember(assignment: MissionAssignment) {
        require(assignment.id.isNotBlank() && assignment.missionId.isNotBlank() && assignment.memberId.isNotBlank())
        dao.saveAssignment(assignment.toEntity())
    }

    override suspend fun updateProgress(assignmentId: String, note: String?, completed: Boolean, updatedAt: Long) {
        dao.updateProgress(
            assignmentId,
            if (completed) MissionAssignmentStatus.COMPLETED.name else MissionAssignmentStatus.IN_PROGRESS.name,
            note?.trim()?.takeIf(String::isNotEmpty),
            updatedAt,
        )
    }

    override suspend fun approveAndReward(assignmentId: String, approvedAt: Long): Boolean =
        dao.approveAndReward(assignmentId, approvedAt)
}

private fun MissionEntity.toDomain() = Mission(
    id, MissionKind.valueOf(kind), title, description,
    EconomyCurrency.valueOf(rewardCurrency), rewardAmount, isActive, createdAtEpochMillis,
)

private fun Mission.toEntity() = MissionEntity(
    id, kind.name, title.trim(), description?.trim()?.takeIf(String::isNotEmpty),
    rewardCurrency.name, rewardAmount, isActive, createdAtEpochMillis,
)

private fun MissionAssignmentEntity.toDomain() = MissionAssignment(
    id, missionId, memberId, MissionAssignmentStatus.valueOf(status), coachProgressNote, updatedAtEpochMillis,
)

private fun MissionAssignment.toEntity() = MissionAssignmentEntity(
    id, missionId, memberId, status.name, coachProgressNote, updatedAtEpochMillis,
)

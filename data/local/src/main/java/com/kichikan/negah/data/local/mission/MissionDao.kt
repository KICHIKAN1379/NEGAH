package com.kichikan.negah.data.local.mission

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.domain.mission.MissionAssignmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY createdAtEpochMillis DESC")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM mission_assignments WHERE missionId = :missionId ORDER BY updatedAtEpochMillis DESC")
    fun observeAssignments(missionId: String): Flow<List<MissionAssignmentEntity>>

    @Query("SELECT * FROM mission_assignments WHERE id = :assignmentId LIMIT 1")
    suspend fun findAssignment(assignmentId: String): MissionAssignmentEntity?

    @Query("SELECT * FROM missions WHERE id = :missionId LIMIT 1")
    suspend fun findMission(missionId: String): MissionEntity?

    @Upsert
    suspend fun saveMission(mission: MissionEntity)

    @Upsert
    suspend fun saveAssignment(assignment: MissionAssignmentEntity)

    @Query("""
        UPDATE mission_assignments
        SET status = :status, coachProgressNote = :note, updatedAtEpochMillis = :updatedAt
        WHERE id = :assignmentId AND status != 'APPROVED'
    """)
    suspend fun updateProgress(assignmentId: String, status: String, note: String?, updatedAt: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM economy_transactions WHERE id = :transactionId)")
    suspend fun rewardExists(transactionId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReward(transaction: EconomyTransactionEntity)

    @Query("""
        UPDATE mission_assignments SET status = 'APPROVED', updatedAtEpochMillis = :approvedAt
        WHERE id = :assignmentId AND status = 'COMPLETED'
    """)
    suspend fun markApproved(assignmentId: String, approvedAt: Long): Int

    @Query("""
        UPDATE members SET
            totalXp = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'XP'),
            spendablePoints = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'POINTS'),
            diamonds = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'DIAMONDS'),
            updatedAtEpochMillis = :updatedAt
        WHERE id = :memberId
    """)
    suspend fun refreshMemberBalances(memberId: String, updatedAt: Long)

    @Transaction
    suspend fun approveAndReward(assignmentId: String, approvedAt: Long): Boolean {
        val assignment = findAssignment(assignmentId) ?: return false
        if (assignment.status != MissionAssignmentStatus.COMPLETED.name) return false
        val mission = findMission(assignment.missionId) ?: return false
        if (mission.rewardAmount <= 0L) return false

        val transactionId = "mission_reward:$assignmentId"
        if (rewardExists(transactionId)) {
            markApproved(assignmentId, approvedAt)
            return false
        }

        insertReward(
            EconomyTransactionEntity(
                id = transactionId,
                memberId = assignment.memberId,
                currency = mission.rewardCurrency,
                amount = mission.rewardAmount,
                transactionType = "EARN",
                sourceType = "MISSION",
                sourceId = mission.id,
                createdAtEpochMillis = approvedAt,
            )
        )
        if (markApproved(assignmentId, approvedAt) != 1) {
            error("Mission assignment changed during reward approval")
        }
        refreshMemberBalances(assignment.memberId, approvedAt)
        return true
    }
}

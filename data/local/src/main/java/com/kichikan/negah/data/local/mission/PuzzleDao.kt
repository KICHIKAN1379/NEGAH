package com.kichikan.negah.data.local.mission

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kichikan.negah.data.local.group.GroupEconomyTransactionEntity
import com.kichikan.negah.domain.mission.CompletePuzzlePieceResult
import com.kichikan.negah.domain.mission.MissionKind
import com.kichikan.negah.domain.mission.PUZZLE_PIECE_COUNT
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    @Query("SELECT * FROM puzzle_pieces WHERE missionId = :missionId ORDER BY groupId, pieceIndex")
    fun observePieces(missionId: String): Flow<List<PuzzlePieceEntity>>

    @Query("SELECT * FROM missions WHERE id = :missionId LIMIT 1")
    suspend fun findMission(missionId: String): MissionEntity?

    @Query("SELECT COUNT(*) FROM member_groups WHERE id = :groupId AND isActive = 1")
    suspend fun activeGroupCount(groupId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPiece(piece: PuzzlePieceEntity): Long

    @Query("SELECT COUNT(*) FROM puzzle_pieces WHERE missionId = :missionId AND groupId = :groupId")
    suspend fun pieceCount(missionId: String, groupId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroupTransaction(transaction: GroupEconomyTransactionEntity)

    @Query("UPDATE member_groups SET spendablePoints = spendablePoints + :delta, updatedAtEpochMillis = :at WHERE id = :groupId")
    suspend fun addGroupPoints(groupId: String, delta: Long, at: Long)

    @Query("UPDATE member_groups SET diamonds = diamonds + :delta, updatedAtEpochMillis = :at WHERE id = :groupId")
    suspend fun addGroupDiamonds(groupId: String, delta: Long, at: Long)

    @Transaction
    suspend fun completePiece(missionId: String, groupId: String, pieceIndex: Int, now: Long): CompletePuzzlePieceResult {
        if (pieceIndex !in 1..PUZZLE_PIECE_COUNT) return CompletePuzzlePieceResult.InvalidPiece
        val mission = findMission(missionId)
        if (mission == null || !mission.isActive || mission.kind != MissionKind.GROUP_PUZZLE.name || activeGroupCount(groupId) != 1) {
            return CompletePuzzlePieceResult.InvalidPuzzleOrGroup
        }
        if (insertPiece(PuzzlePieceEntity(missionId, groupId, pieceIndex, now)) == -1L) {
            return CompletePuzzlePieceResult.AlreadyCompleted
        }
        val done = pieceCount(missionId, groupId)
        var rewarded = false
        if (done == PUZZLE_PIECE_COUNT && mission.rewardAmount > 0L && mission.rewardCurrency in setOf("POINTS", "DIAMONDS")) {
            insertGroupTransaction(
                GroupEconomyTransactionEntity(
                    id = "puzzle_reward:$missionId:$groupId",
                    groupId = groupId,
                    currency = mission.rewardCurrency,
                    amountDelta = mission.rewardAmount,
                    reason = "تکمیل پازل گروهی: ${mission.title}",
                    createdAtEpochMillis = now,
                ),
            )
            if (mission.rewardCurrency == "POINTS") addGroupPoints(groupId, mission.rewardAmount, now)
            else addGroupDiamonds(groupId, mission.rewardAmount, now)
            rewarded = true
        }
        return CompletePuzzlePieceResult.Completed(done, rewarded)
    }
}

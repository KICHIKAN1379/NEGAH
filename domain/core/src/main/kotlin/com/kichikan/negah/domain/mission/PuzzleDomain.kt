package com.kichikan.negah.domain.mission

import kotlinx.coroutines.flow.Flow

const val PUZZLE_PIECE_COUNT = 6

data class PuzzlePiece(val missionId: String, val groupId: String, val pieceIndex: Int, val completedAtEpochMillis: Long)

sealed interface CompletePuzzlePieceResult {
    /** [rewarded] is true only on the call that completed the sixth piece and paid the group. */
    data class Completed(val piecesDone: Int, val rewarded: Boolean) : CompletePuzzlePieceResult
    data object AlreadyCompleted : CompletePuzzlePieceResult
    data object InvalidPiece : CompletePuzzlePieceResult
    data object InvalidPuzzleOrGroup : CompletePuzzlePieceResult
}

interface PuzzleRepository {
    fun observePieces(missionId: String): Flow<List<PuzzlePiece>>

    /**
     * Records one piece for one group. When it is the sixth piece the mission reward
     * (points or diamonds) is paid once to the group's own ledger, in the same transaction.
     */
    suspend fun completePiece(missionId: String, groupId: String, pieceIndex: Int, nowEpochMillis: Long): CompletePuzzlePieceResult
}

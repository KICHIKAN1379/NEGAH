package com.kichikan.negah.data.local.mission

import com.kichikan.negah.domain.mission.CompletePuzzlePieceResult
import com.kichikan.negah.domain.mission.PuzzlePiece
import com.kichikan.negah.domain.mission.PuzzleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPuzzleRepository @Inject constructor(private val dao: PuzzleDao) : PuzzleRepository {
    override fun observePieces(missionId: String): Flow<List<PuzzlePiece>> =
        dao.observePieces(missionId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun completePiece(missionId: String, groupId: String, pieceIndex: Int, nowEpochMillis: Long): CompletePuzzlePieceResult =
        dao.completePiece(missionId, groupId, pieceIndex, nowEpochMillis)
}

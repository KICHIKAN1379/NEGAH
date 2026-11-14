package com.kichikan.negah.data.local.mission

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.kichikan.negah.data.local.group.MemberGroupEntity
import com.kichikan.negah.domain.mission.PuzzlePiece

@Entity(
    tableName = "puzzle_pieces",
    primaryKeys = ["missionId", "groupId", "pieceIndex"],
    foreignKeys = [
        ForeignKey(entity = MissionEntity::class, parentColumns = ["id"], childColumns = ["missionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MemberGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("groupId")],
)
data class PuzzlePieceEntity(
    val missionId: String,
    val groupId: String,
    val pieceIndex: Int,
    val completedAtEpochMillis: Long,
) {
    fun toDomain() = PuzzlePiece(missionId, groupId, pieceIndex, completedAtEpochMillis)
}

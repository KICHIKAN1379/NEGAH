package com.kichikan.negah.data.local.mission

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val description: String?,
    val rewardCurrency: String,
    val rewardAmount: Long,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "mission_assignments",
    foreignKeys = [
        ForeignKey(entity = MissionEntity::class, parentColumns = ["id"], childColumns = ["missionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("missionId"), Index("memberId"), Index(value = ["missionId", "memberId"], unique = true)],
)
data class MissionAssignmentEntity(
    @PrimaryKey val id: String,
    val missionId: String,
    val memberId: String,
    val status: String,
    val coachProgressNote: String?,
    val updatedAtEpochMillis: Long,
)

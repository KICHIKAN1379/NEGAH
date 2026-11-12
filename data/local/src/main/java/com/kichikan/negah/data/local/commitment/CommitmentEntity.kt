package com.kichikan.negah.data.local.commitment

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity

@Entity(
    tableName = "commitments",
    foreignKeys = [ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("memberId")],
)
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val topic: String,
    val description: String,
    val commitmentDateEpochDay: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

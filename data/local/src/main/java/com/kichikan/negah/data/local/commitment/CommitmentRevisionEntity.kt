package com.kichikan.negah.data.local.commitment

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "commitment_revisions",
    foreignKeys = [ForeignKey(entity = CommitmentEntity::class, parentColumns = ["id"], childColumns = ["commitmentId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("commitmentId")],
)
data class CommitmentRevisionEntity(
    @PrimaryKey val id: String,
    val commitmentId: String,
    val previousTopic: String,
    val previousDescription: String,
    val revisedAtEpochMillis: Long,
)

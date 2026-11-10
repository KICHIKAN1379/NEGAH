package com.kichikan.negah.data.local.member

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val nickname: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val isArchived: Boolean = false,
)

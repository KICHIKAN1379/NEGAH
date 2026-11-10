package com.kichikan.negah.data.local.session

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sessionDateEpochDay: Long,
    val notes: String?,
    val createdAtEpochMillis: Long,
)

package com.kichikan.negah.data.local.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ring_profile")
data class RingProfileEntity(
    @PrimaryKey val id: String = PRIMARY_ID,
    val name: String,
    val username: String?,
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val PRIMARY_ID = "primary"
    }
}

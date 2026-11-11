package com.kichikan.negah.domain.settings

import kotlinx.coroutines.flow.Flow

data class RingProfile(
    val name: String = "",
    val username: String? = null,
    val updatedAtEpochMillis: Long = 0L,
)

interface RingProfileRepository {
    fun observeProfile(): Flow<RingProfile>
    suspend fun saveProfile(profile: RingProfile)
}

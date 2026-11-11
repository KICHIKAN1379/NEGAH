package com.kichikan.negah.data.local.settings

import com.kichikan.negah.domain.settings.RingProfile
import com.kichikan.negah.domain.settings.RingProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRingProfileRepository @Inject constructor(
    private val dao: RingProfileDao,
) : RingProfileRepository {
    override fun observeProfile(): Flow<RingProfile> =
        dao.observePrimary().map { entity ->
            entity?.let {
                RingProfile(
                    name = it.name,
                    username = it.username,
                    updatedAtEpochMillis = it.updatedAtEpochMillis,
                )
            } ?: RingProfile()
        }

    override suspend fun saveProfile(profile: RingProfile) {
        dao.save(
            RingProfileEntity(
                name = profile.name,
                username = profile.username,
                updatedAtEpochMillis = profile.updatedAtEpochMillis,
            ),
        )
    }
}

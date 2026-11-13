package com.kichikan.negah.data.local.growth

import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthMapRepository
import com.kichikan.negah.domain.growth.GrowthProgressChange
import com.kichikan.negah.domain.growth.GrowthTrack
import com.kichikan.negah.domain.growth.MemberGrowthProgress
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGrowthMapRepository @Inject constructor(
    private val dao: GrowthMapDao,
) : GrowthMapRepository {
    override fun observeItems(track: GrowthTrack): Flow<List<GrowthItem>> =
        dao.observeItems(track.name).map { rows -> rows.map(GrowthItemEntity::toDomain) }

    override fun observeAllItems(track: GrowthTrack): Flow<List<GrowthItem>> =
        dao.observeAllItems(track.name).map { rows -> rows.map(GrowthItemEntity::toDomain) }

    override fun observeProgress(memberId: String): Flow<List<MemberGrowthProgress>> =
        dao.observeProgress(memberId).map { rows -> rows.map(MemberGrowthProgressEntity::toDomain) }

    override suspend fun saveItem(item: GrowthItem) =
        dao.upsertItem(GrowthItemEntity.fromDomain(item))

    override fun observeHistory(memberId: String): Flow<List<GrowthProgressChange>> =
        dao.observeHistory(memberId).map { rows -> rows.map(MemberGrowthHistoryEntity::toDomain) }

    override suspend fun updateProgress(progress: MemberGrowthProgress) =
        dao.saveProgressWithHistory(
            MemberGrowthProgressEntity.fromDomain(progress),
            historyId = UUID.randomUUID().toString(),
        )
}

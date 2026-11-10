package com.kichikan.negah.data.local.growth

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthMapDao {
    @Query("SELECT * FROM growth_items WHERE track = :track AND isActive = 1 ORDER BY sortOrder, title COLLATE NOCASE")
    fun observeItems(track: String): Flow<List<GrowthItemEntity>>

    @Query("SELECT * FROM member_growth_progress WHERE memberId = :memberId ORDER BY updatedAtEpochMillis DESC")
    fun observeProgress(memberId: String): Flow<List<MemberGrowthProgressEntity>>

    @Upsert
    suspend fun upsertItem(item: GrowthItemEntity)

    @Upsert
    suspend fun upsertProgress(progress: MemberGrowthProgressEntity)
}

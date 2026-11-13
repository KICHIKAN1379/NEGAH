package com.kichikan.negah.data.local.growth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthMapDao {
    @Query("SELECT * FROM growth_items WHERE track = :track AND isActive = 1 ORDER BY sortOrder, title COLLATE NOCASE")
    fun observeItems(track: String): Flow<List<GrowthItemEntity>>

    @Query("SELECT * FROM growth_items WHERE track = :track ORDER BY sortOrder, title COLLATE NOCASE")
    fun observeAllItems(track: String): Flow<List<GrowthItemEntity>>

    @Query("SELECT * FROM member_growth_progress WHERE memberId = :memberId ORDER BY updatedAtEpochMillis DESC")
    fun observeProgress(memberId: String): Flow<List<MemberGrowthProgressEntity>>

    @Upsert
    suspend fun upsertItem(item: GrowthItemEntity)

    @Upsert
    suspend fun upsertProgress(progress: MemberGrowthProgressEntity)

    @Query("SELECT * FROM member_growth_progress WHERE memberId = :memberId AND growthItemId = :growthItemId LIMIT 1")
    suspend fun findProgress(memberId: String, growthItemId: String): MemberGrowthProgressEntity?

    @Insert
    suspend fun insertHistory(change: MemberGrowthHistoryEntity)

    @Query("SELECT * FROM member_growth_history WHERE memberId = :memberId ORDER BY changedAtEpochMillis DESC, id")
    fun observeHistory(memberId: String): Flow<List<MemberGrowthHistoryEntity>>

    /** Saves the new value and appends an immutable history row when something actually changed. */
    @Transaction
    suspend fun saveProgressWithHistory(progress: MemberGrowthProgressEntity, historyId: String) {
        val previous = findProgress(progress.memberId, progress.growthItemId)
        upsertProgress(progress)
        if (previous != null && previous.progressPercent == progress.progressPercent && previous.note == progress.note) return
        insertHistory(
            MemberGrowthHistoryEntity(
                id = historyId,
                memberId = progress.memberId,
                growthItemId = progress.growthItemId,
                previousPercent = previous?.progressPercent,
                newPercent = progress.progressPercent,
                note = progress.note,
                changedAtEpochMillis = progress.updatedAtEpochMillis,
            ),
        )
    }
}

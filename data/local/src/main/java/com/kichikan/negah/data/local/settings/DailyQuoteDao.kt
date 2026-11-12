package com.kichikan.negah.data.local.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuoteDao {
    @Query("SELECT * FROM daily_quotes ORDER BY createdAtEpochMillis ASC, id ASC")
    fun observeAll(): Flow<List<DailyQuoteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(quote: DailyQuoteEntity)
    @Query("DELETE FROM daily_quotes WHERE id = :id")
    suspend fun delete(id: String)
}
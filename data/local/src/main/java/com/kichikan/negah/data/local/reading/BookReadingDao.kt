package com.kichikan.negah.data.local.reading

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookReadingDao {
    @Query("SELECT * FROM book_readings ORDER BY readAtEpochMillis DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<BookReadingEntity>>

    @Insert
    suspend fun insert(reading: BookReadingEntity)
}

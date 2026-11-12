package com.kichikan.negah.data.local.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RingProfileDao {
    @Query("SELECT * FROM ring_profile WHERE id = 'primary' LIMIT 1")
    fun observePrimary(): Flow<RingProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: RingProfileEntity)
}

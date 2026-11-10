package com.kichikan.negah.data.local.session

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY sessionDateEpochDay DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun findById(sessionId: String): SessionEntity?

    @Upsert
    suspend fun upsert(session: SessionEntity)
}

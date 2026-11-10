package com.kichikan.negah.data.local.session

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY sessionDateEpochDay ASC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun findById(sessionId: String): SessionEntity?

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String): Int

    @Query("UPDATE sessions SET isCompleted = :isCompleted WHERE id = :sessionId")
    suspend fun setCompleted(sessionId: String, isCompleted: Boolean): Int
}

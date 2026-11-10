package com.kichikan.negah.data.local.attendance

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY memberId")
    fun observeForSession(sessionId: String): Flow<List<AttendanceEntity>>

    @Upsert
    suspend fun upsert(record: AttendanceEntity)
}

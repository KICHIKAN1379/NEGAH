package com.kichikan.negah.data.local.member

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE isArchived = 0 ORDER BY fullName COLLATE NOCASE")
    fun observeActiveMembers(): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members WHERE id = :memberId LIMIT 1")
    suspend fun findById(memberId: String): MemberEntity?
    @Upsert suspend fun upsert(member: MemberEntity)
    @Query("UPDATE members SET isArchived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :memberId")
    suspend fun archive(memberId: String, updatedAt: Long)
}

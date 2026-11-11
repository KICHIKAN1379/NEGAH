package com.kichikan.negah.data.local.member

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE isArchived = 0 ORDER BY fullName COLLATE NOCASE")
    fun observeActiveMembers(): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members WHERE isArchived = 1 ORDER BY fullName COLLATE NOCASE")
    fun observeArchivedMembers(): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members WHERE id = :memberId LIMIT 1")
    suspend fun findById(memberId: String): MemberEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(member: MemberEntity)
    @Query("UPDATE members SET birthDateJalali = :birthDateJalali, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :memberId")
    suspend fun updateBirthDate(memberId: String, birthDateJalali: String?, updatedAtEpochMillis: Long)
    @Query("UPDATE members SET isArchived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :memberId AND isArchived = 0")
    suspend fun archive(memberId: String, updatedAt: Long)
    @Query("UPDATE members SET isArchived = 0, updatedAtEpochMillis = :updatedAt WHERE id = :memberId AND isArchived = 1")
    suspend fun restore(memberId: String, updatedAt: Long)
}

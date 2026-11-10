package com.kichikan.negah.data.local.commitment

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments WHERE memberId = :memberId ORDER BY commitmentDateEpochDay DESC")
    fun observeForMember(memberId: String): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitment_revisions WHERE commitmentId = :commitmentId ORDER BY revisedAtEpochMillis DESC")
    fun observeRevisions(commitmentId: String): Flow<List<CommitmentRevisionEntity>>

    @Upsert
    suspend fun upsert(commitment: CommitmentEntity)

    @Upsert
    suspend fun addRevision(revision: CommitmentRevisionEntity)
}

package com.kichikan.negah.data.local.economy

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyTransactionDao {
    @Query("SELECT * FROM economy_transactions WHERE memberId = :memberId ORDER BY createdAtEpochMillis DESC")
    fun observeForMember(memberId: String): Flow<List<EconomyTransactionEntity>>
}

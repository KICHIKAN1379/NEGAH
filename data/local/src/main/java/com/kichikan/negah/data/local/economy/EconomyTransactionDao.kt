package com.kichikan.negah.data.local.economy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kichikan.negah.domain.economy.LedgerAppendResult
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyTransactionDao {
    @Query("SELECT * FROM economy_transactions WHERE memberId = :memberId ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeForMember(memberId: String): Flow<List<EconomyTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = :currency")
    suspend fun balanceFor(memberId: String, currency: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: EconomyTransactionEntity)

    @Query("""
        UPDATE members SET
            totalXp = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'XP'),
            spendablePoints = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'POINTS'),
            diamonds = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'DIAMONDS'),
            updatedAtEpochMillis = :updatedAt
        WHERE id = :memberId
    """)
    suspend fun refreshMemberBalances(memberId: String, updatedAt: Long)

    /**
     * Appends the immutable ledger row and refreshes the cached member balances
     * in one SQLite transaction. A failed balance check writes nothing.
     */
    @Transaction
    suspend fun appendAtomically(transaction: EconomyTransactionEntity): LedgerAppendResult {
        val current = balanceFor(transaction.memberId, transaction.currency)
        val next = try {
            Math.addExact(current, transaction.amount)
        } catch (_: ArithmeticException) {
            return LedgerAppendResult.BALANCE_OVERFLOW
        }
        if (next < 0L) return LedgerAppendResult.INSUFFICIENT_BALANCE

        insert(transaction)
        refreshMemberBalances(transaction.memberId, transaction.createdAtEpochMillis)
        return LedgerAppendResult.APPENDED
    }
}

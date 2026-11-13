package com.kichikan.negah.data.local.group

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kichikan.negah.domain.group.GroupEconomyAppendResult
import kotlinx.coroutines.flow.Flow

data class GroupEconomyBalanceRow(
    val spendablePoints: Long,
    val diamonds: Long,
)

@Dao
interface GroupEconomyDao {
    @Query(
        "SELECT * FROM group_economy_transactions WHERE groupId = :groupId " +
            "ORDER BY createdAtEpochMillis DESC, id DESC",
    )
    fun observeForGroup(groupId: String): Flow<List<GroupEconomyTransactionEntity>>

    @Query("SELECT spendablePoints, diamonds FROM member_groups WHERE id = :groupId")
    suspend fun balanceFor(groupId: String): GroupEconomyBalanceRow?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<GroupEconomyTransactionEntity>)

    @Query(
        """
        UPDATE member_groups
        SET spendablePoints = :spendablePoints,
            diamonds = :diamonds,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :groupId
        """,
    )
    suspend fun updateBalances(
        groupId: String,
        spendablePoints: Long,
        diamonds: Long,
        updatedAt: Long,
    )

    /** Ledger append and cached balances are committed together or not at all. */
    @Transaction
    suspend fun appendAtomically(
        transactions: List<GroupEconomyTransactionEntity>,
    ): GroupEconomyAppendResult {
        if (transactions.isEmpty() ||
            transactions.any { it.groupId != transactions.first().groupId || it.amountDelta == 0L || it.reason.isBlank() } ||
            transactions.map { it.currency }.distinct().size != transactions.size
        ) return GroupEconomyAppendResult.INVALID_BATCH

        val groupId = transactions.first().groupId
        val current = balanceFor(groupId) ?: return GroupEconomyAppendResult.GROUP_NOT_FOUND
        val pointsDelta = transactions.filter { it.currency == "POINTS" }.sumOf { it.amountDelta }
        val diamondsDelta = transactions.filter { it.currency == "DIAMONDS" }.sumOf { it.amountDelta }
        val nextPoints = try {
            Math.addExact(current.spendablePoints, pointsDelta)
        } catch (_: ArithmeticException) {
            return GroupEconomyAppendResult.BALANCE_OVERFLOW
        }
        val nextDiamonds = try {
            Math.addExact(current.diamonds, diamondsDelta)
        } catch (_: ArithmeticException) {
            return GroupEconomyAppendResult.BALANCE_OVERFLOW
        }
        if (nextPoints < 0L || nextDiamonds < 0L) {
            return GroupEconomyAppendResult.INSUFFICIENT_BALANCE
        }

        insertAll(transactions)
        updateBalances(
            groupId = groupId,
            spendablePoints = nextPoints,
            diamonds = nextDiamonds,
            updatedAt = transactions.maxOf { it.createdAtEpochMillis },
        )
        return GroupEconomyAppendResult.APPENDED
    }
}

package com.kichikan.negah.data.local.wheel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.domain.wheel.WheelPicker
import com.kichikan.negah.domain.wheel.WheelSpinResult
import kotlinx.coroutines.flow.Flow

@Dao
interface WheelDao {
    @Query("SELECT * FROM wheel_prizes ORDER BY createdAtEpochMillis, id")
    fun observePrizes(): Flow<List<WheelPrizeEntity>>

    @Query("SELECT * FROM wheel_prizes WHERE isActive = 1 ORDER BY createdAtEpochMillis, id")
    suspend fun activePrizes(): List<WheelPrizeEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrize(prize: WheelPrizeEntity)

    /** In-place update: REPLACE would delete the row and trip the RESTRICT key from past spins. */
    @Query("UPDATE wheel_prizes SET label = :label, currency = :currency, amount = :amount, weight = :weight, isActive = :isActive, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updatePrize(id: String, label: String, currency: String, amount: Long, weight: Int, isActive: Boolean, updatedAt: Long): Int

    @Query("SELECT spinCostPoints FROM wheel_settings WHERE id = 1")
    fun observeSpinCost(): Flow<Long?>

    @Query("SELECT spinCostPoints FROM wheel_settings WHERE id = 1")
    suspend fun spinCost(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: WheelSettingsEntity)

    @Query("SELECT * FROM wheel_spins ORDER BY spunAtEpochMillis DESC, id DESC")
    fun observeSpins(): Flow<List<WheelSpinEntity>>

    @Query("SELECT COUNT(*) FROM members WHERE id = :memberId AND isArchived = 0")
    suspend fun activeMemberCount(memberId: String): Int

    @Query("SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = :currency")
    suspend fun balanceFor(memberId: String, currency: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: EconomyTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpin(spin: WheelSpinEntity)

    @Query("""
        UPDATE members SET
            totalXp = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'XP'),
            spendablePoints = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'POINTS'),
            diamonds = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'DIAMONDS'),
            updatedAtEpochMillis = :updatedAt
        WHERE id = :memberId
    """)
    suspend fun refreshMemberBalances(memberId: String, updatedAt: Long)

    @Transaction
    suspend fun savePrize(prize: WheelPrizeEntity) {
        val updated = updatePrize(prize.id, prize.label, prize.currency, prize.amount, prize.weight, prize.isActive, prize.updatedAtEpochMillis)
        if (updated == 0) insertPrize(prize)
    }

    /**
     * Debit the spin cost, credit the reward and record the spin in ONE transaction.
     * Any failure (insufficient points, overflow) writes nothing.
     */
    @Transaction
    suspend fun spinAtomically(memberId: String, spinId: String, now: Long, roll: (Long) -> Long): WheelSpinResult {
        if (activeMemberCount(memberId) != 1) return WheelSpinResult.InvalidMember
        val prizes = activePrizes().map { it.toDomain() }
        val total = WheelPicker.totalWeight(prizes)
        if (total <= 0L) return WheelSpinResult.NoActivePrizes
        val prize = WheelPicker.pick(prizes, roll(total)) ?: return WheelSpinResult.NoActivePrizes

        val cost = spinCost() ?: 0L
        val points = balanceFor(memberId, "POINTS")
        if (points < cost) return WheelSpinResult.InsufficientPoints

        val rewardBase = if (prize.currency.name == "POINTS") points - cost else balanceFor(memberId, prize.currency.name)
        try {
            Math.addExact(rewardBase, prize.amount)
        } catch (_: ArithmeticException) {
            return WheelSpinResult.BalanceOverflow
        }

        if (cost > 0L) {
            insertTransaction(
                EconomyTransactionEntity("wheel_cost:$spinId", memberId, "POINTS", -cost, "SPEND", "WHEEL", spinId, now),
            )
        }
        insertTransaction(
            EconomyTransactionEntity("wheel_reward:$spinId", memberId, prize.currency.name, prize.amount, "EARN", "WHEEL", spinId, now),
        )
        val spin = WheelSpinEntity(spinId, memberId, prize.id, prize.label, prize.currency.name, prize.amount, cost, now)
        insertSpin(spin)
        refreshMemberBalances(memberId, now)
        return WheelSpinResult.Won(spin.toDomain())
    }
}

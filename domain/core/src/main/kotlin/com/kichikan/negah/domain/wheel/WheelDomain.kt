package com.kichikan.negah.domain.wheel

import com.kichikan.negah.domain.economy.EconomyCurrency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** A prize on the wheel. Prizes are never deleted so spin history stays explainable; they are deactivated. */
data class WheelPrize(
    val id: String,
    val label: String,
    val currency: EconomyCurrency,
    val amount: Long,
    val weight: Int,
    val isActive: Boolean,
)

/** One completed spin. The label and amounts are copied so later prize edits never rewrite history. */
data class WheelSpin(
    val id: String,
    val memberId: String,
    val prizeId: String,
    val prizeLabel: String,
    val currency: EconomyCurrency,
    val amount: Long,
    val costPoints: Long,
    val spunAtEpochMillis: Long,
)

sealed interface WheelSpinResult {
    data class Won(val spin: WheelSpin) : WheelSpinResult
    data object NoActivePrizes : WheelSpinResult
    data object InsufficientPoints : WheelSpinResult
    data object InvalidMember : WheelSpinResult
    data object BalanceOverflow : WheelSpinResult
}

object WheelPicker {
    fun totalWeight(prizes: List<WheelPrize>): Long =
        prizes.filter { it.isActive && it.weight > 0 }.sumOf { it.weight.toLong() }

    /** [roll] must be in 0 until [totalWeight]; returns null when there is nothing to pick. */
    fun pick(prizes: List<WheelPrize>, roll: Long): WheelPrize? {
        val candidates = prizes.filter { it.isActive && it.weight > 0 }
        val total = candidates.sumOf { it.weight.toLong() }
        if (total <= 0L || roll !in 0 until total) return null
        var cursor = roll
        for (prize in candidates) {
            if (cursor < prize.weight) return prize
            cursor -= prize.weight
        }
        return null
    }
}

interface WheelRepository {
    fun observePrizes(): Flow<List<WheelPrize>>
    fun observeSpinCost(): Flow<Long>
    fun observeSpins(): Flow<List<WheelSpin>>
    suspend fun savePrize(prize: WheelPrize, nowEpochMillis: Long)
    suspend fun setSpinCost(costPoints: Long)
    /** Debits the cost, credits the reward and writes the spin row in one transaction. */
    suspend fun spin(memberId: String, spinId: String, nowEpochMillis: Long, roll: (Long) -> Long): WheelSpinResult
}

sealed interface SavePrizeResult {
    data object Saved : SavePrizeResult
    data object InvalidLabel : SavePrizeResult
    data object InvalidAmount : SavePrizeResult
    data object InvalidWeight : SavePrizeResult
}

class SaveWheelPrize @Inject constructor(private val repository: WheelRepository) {
    suspend operator fun invoke(prize: WheelPrize, nowEpochMillis: Long): SavePrizeResult {
        if (prize.id.isBlank() || prize.label.isBlank()) return SavePrizeResult.InvalidLabel
        if (prize.amount <= 0L) return SavePrizeResult.InvalidAmount
        if (prize.weight <= 0) return SavePrizeResult.InvalidWeight
        repository.savePrize(prize.copy(label = prize.label.trim()), nowEpochMillis)
        return SavePrizeResult.Saved
    }
}

package com.kichikan.negah.domain.wheel

import com.kichikan.negah.domain.economy.EconomyCurrency
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WheelDomainTest {
    private fun prize(id: String, weight: Int, active: Boolean = true) =
        WheelPrize(id, id, EconomyCurrency.POINTS, 10, weight, active)

    @Test
    fun pickFollowsWeightRanges() {
        val prizes = listOf(prize("a", 2), prize("b", 3), prize("c", 5))
        assertEquals(10L, WheelPicker.totalWeight(prizes))
        assertEquals("a", WheelPicker.pick(prizes, 0)?.id)
        assertEquals("a", WheelPicker.pick(prizes, 1)?.id)
        assertEquals("b", WheelPicker.pick(prizes, 2)?.id)
        assertEquals("b", WheelPicker.pick(prizes, 4)?.id)
        assertEquals("c", WheelPicker.pick(prizes, 5)?.id)
        assertEquals("c", WheelPicker.pick(prizes, 9)?.id)
    }

    @Test
    fun inactiveAndOutOfRangeNeverWin() {
        val prizes = listOf(prize("a", 4, active = false), prize("b", 1))
        assertEquals(1L, WheelPicker.totalWeight(prizes))
        assertEquals("b", WheelPicker.pick(prizes, 0)?.id)
        assertNull(WheelPicker.pick(prizes, 1))
        assertNull(WheelPicker.pick(prizes, -1))
        assertNull(WheelPicker.pick(emptyList(), 0))
    }

    @Test
    fun saveRejectsInvalidPrizes() = runBlocking {
        var saved = 0
        val repo = object : WheelRepository {
            override fun observePrizes(): Flow<List<WheelPrize>> = emptyFlow()
            override fun observeSpinCost(): Flow<Long> = emptyFlow()
            override fun observeSpins(): Flow<List<WheelSpin>> = emptyFlow()
            override suspend fun savePrize(prize: WheelPrize, nowEpochMillis: Long) { saved++ }
            override suspend fun setSpinCost(costPoints: Long) {}
            override suspend fun spin(memberId: String, spinId: String, nowEpochMillis: Long, roll: (Long) -> Long) =
                WheelSpinResult.NoActivePrizes
        }
        val save = SaveWheelPrize(repo)
        assertEquals(SavePrizeResult.InvalidLabel, save(prize("p", 1).copy(label = "  "), 1L))
        assertEquals(SavePrizeResult.InvalidAmount, save(prize("p", 1).copy(amount = 0), 1L))
        assertEquals(SavePrizeResult.InvalidWeight, save(prize("p", 0), 1L))
        assertEquals(0, saved)
        assertEquals(SavePrizeResult.Saved, save(prize("p", 1), 1L))
        assertEquals(1, saved)
    }
}

package com.kichikan.negah.domain.economy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RankBadgeProgressionTest {
    @Test
    fun levelZeroHasNoRankBadge() {
        assertEquals(emptyList(), RankBadgeProgression.earnedForLevel(0))
        assertNull(RankBadgeProgression.currentForLevel(0))
    }

    @Test
    fun eachLevelUnlocksExactlyItsMatchingRank() {
        assertEquals("سرباز", RankBadgeProgression.currentForLevel(1)?.title)
        assertEquals("سرباز دوم", RankBadgeProgression.currentForLevel(2)?.title)
        assertEquals(10, RankBadgeProgression.earnedForLevel(10).size)
    }

    @Test
    fun levelTwentyOneUnlocksFinalRankAndHigherLevelsStayCapped() {
        assertEquals(21, RankBadgeProgression.earnedForLevel(21).size)
        assertEquals("ارتشبد", RankBadgeProgression.currentForLevel(21)?.title)
        assertEquals(21, RankBadgeProgression.earnedForLevel(99).size)
        assertEquals("ارتشبد", RankBadgeProgression.currentForLevel(99)?.title)
    }
}

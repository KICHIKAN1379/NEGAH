package com.kichikan.negah.domain.economy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LevelProgressionTest {
    @Test
    fun levelBoundariesAreCorrect() {
        assertEquals(0, LevelProgression.levelFor(0))
        assertEquals(0, LevelProgression.levelFor(19))
        assertEquals(1, LevelProgression.levelFor(20))
        assertEquals(1, LevelProgression.levelFor(49))
        assertEquals(2, LevelProgression.levelFor(50))
        assertEquals(2, LevelProgression.levelFor(79))
        assertEquals(3, LevelProgression.levelFor(80))
        assertEquals(3, LevelProgression.levelFor(109))
        assertEquals(4, LevelProgression.levelFor(110))
    }

    @Test
    fun negativeXpIsTreatedAsZero() {
        assertEquals(0, LevelProgression.levelFor(-1))
        assertEquals(0, LevelProgression.levelFor(Long.MIN_VALUE))
    }

    @Test
    fun veryLargeXpDoesNotOverflowTheLevelType() {
        assertEquals(Int.MAX_VALUE, LevelProgression.levelFor(Long.MAX_VALUE))
    }

    @Test
    fun minimumXpForLevelMatchesProgressionBoundaries() {
        assertEquals(0L, LevelProgression.minimumXpForLevel(0))
        assertEquals(20L, LevelProgression.minimumXpForLevel(1))
        assertEquals(50L, LevelProgression.minimumXpForLevel(2))
        assertEquals(80L, LevelProgression.minimumXpForLevel(3))
        assertEquals(110L, LevelProgression.minimumXpForLevel(4))
    }

    @Test
    fun negativeLevelIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            LevelProgression.minimumXpForLevel(-1)
        }
    }
}

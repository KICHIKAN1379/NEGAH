package com.kichikan.negah.domain.economy

import kotlin.test.Test
import kotlin.test.assertEquals

class LevelProgressionTest {
    @Test
    fun levelBoundariesAreCorrect() {
        assertEquals(0, LevelProgression.levelFor(0))
        assertEquals(0, LevelProgression.levelFor(19))
        assertEquals(1, LevelProgression.levelFor(20))
        assertEquals(1, LevelProgression.levelFor(49))
        assertEquals(2, LevelProgression.levelFor(50))
        assertEquals(3, LevelProgression.levelFor(80))
    }
}

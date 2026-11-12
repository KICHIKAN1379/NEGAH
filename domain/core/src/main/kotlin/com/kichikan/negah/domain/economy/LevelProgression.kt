package com.kichikan.negah.domain.economy

object LevelProgression {
    fun levelFor(totalXp: Long): Int {
        val xp = totalXp.coerceAtLeast(0L)
        val calculatedLevel = if (xp < FIRST_LEVEL_XP) {
            0L
        } else {
            1L + ((xp - FIRST_LEVEL_XP) / XP_PER_LEVEL)
        }
        return calculatedLevel.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun minimumXpForLevel(level: Int): Long {
        require(level >= 0) { "Level cannot be negative." }
        return if (level == 0) 0L else FIRST_LEVEL_XP + (level - 1L) * XP_PER_LEVEL
    }

    private const val FIRST_LEVEL_XP = 20L
    private const val XP_PER_LEVEL = 30L
}

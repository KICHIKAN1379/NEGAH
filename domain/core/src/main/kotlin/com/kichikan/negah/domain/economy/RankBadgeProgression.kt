package com.kichikan.negah.domain.economy

data class RankBadge(
    val level: Int,
    val title: String,
)

/**
 * One rank badge is unlocked at each level from 1 through 21.
 * The progression is capped at the final rank even if XP levels continue.
 */
object RankBadgeProgression {
    val all: List<RankBadge> = listOf(
        "سرباز",
        "سرباز دوم",
        "سرباز یکم",
        "رزمیار",
        "رزم‌آور سوم",
        "رزم‌آور دوم",
        "رزم‌آور یکم",
        "رزم‌دار دوم",
        "رزم‌دار یکم",
        "ستوان سوم",
        "ستوان دوم",
        "ستوان یکم",
        "سروان",
        "سرگرد",
        "سرهنگ دوم",
        "سرهنگ",
        "سرتیپ دوم",
        "سرتیپ",
        "سرلشکر",
        "سپهبد",
        "ارتشبد",
    ).mapIndexed { index, title -> RankBadge(level = index + 1, title = title) }

    fun earnedForLevel(level: Int): List<RankBadge> =
        all.take(level.coerceIn(0, all.size))

    fun currentForLevel(level: Int): RankBadge? =
        all.getOrNull(level.coerceIn(0, all.size) - 1)
}

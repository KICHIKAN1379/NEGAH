package com.kichikan.negah.domain.reading

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

object ReadingLevels {
    const val MIN = 1
    const val MAX = 5
}

/** One book the coach recorded for a member. Records are append-only history. */
data class BookReading(
    val id: String,
    val memberId: String,
    val title: String,
    val readAtEpochMillis: Long,
    val level: Int,
    val createdAtEpochMillis: Long,
)

interface ReadingRepository {
    fun observeAll(): Flow<List<BookReading>>
    suspend fun add(reading: BookReading)
}

sealed interface RecordBookReadingResult {
    data class Saved(val reading: BookReading) : RecordBookReadingResult
    data object InvalidMember : RecordBookReadingResult
    data object InvalidTitle : RecordBookReadingResult
    data object InvalidLevel : RecordBookReadingResult
}

class RecordBookReading @Inject constructor(
    private val repository: ReadingRepository,
) {
    suspend operator fun invoke(
        memberId: String,
        title: String,
        readAtEpochMillis: Long,
        level: Int,
        nowEpochMillis: Long,
    ): RecordBookReadingResult {
        if (memberId.isBlank()) return RecordBookReadingResult.InvalidMember
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return RecordBookReadingResult.InvalidTitle
        if (level !in ReadingLevels.MIN..ReadingLevels.MAX) return RecordBookReadingResult.InvalidLevel
        val reading = BookReading(
            id = UUID.randomUUID().toString(),
            memberId = memberId,
            title = cleanTitle,
            readAtEpochMillis = readAtEpochMillis.coerceAtLeast(0L),
            level = level,
            createdAtEpochMillis = nowEpochMillis,
        )
        repository.add(reading)
        return RecordBookReadingResult.Saved(reading)
    }
}

data class ReadingRankEntry(val memberId: String, val score: Long, val bookCount: Int)

/** Reading score = sum of the 1..5 levels of the member's recorded books. */
object ReadingScoreCalculator {
    fun score(readings: List<BookReading>, memberId: String): Long =
        readings.filter { it.memberId == memberId }.sumOf { it.level.toLong() }

    fun ranking(readings: List<BookReading>): List<ReadingRankEntry> =
        readings.groupBy { it.memberId }
            .map { (memberId, rows) ->
                ReadingRankEntry(memberId, rows.sumOf { it.level.toLong() }, rows.size)
            }
            .sortedWith(compareByDescending<ReadingRankEntry> { it.score }.thenByDescending { it.bookCount }.thenBy { it.memberId })
}

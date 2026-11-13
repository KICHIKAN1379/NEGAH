package com.kichikan.negah.domain.reading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

private class RecordingReadingRepository : ReadingRepository {
    val saved = mutableListOf<BookReading>()
    override fun observeAll(): Flow<List<BookReading>> = flowOf(saved.toList())
    override suspend fun add(reading: BookReading) { saved += reading }
}

class ReadingDomainTest {
    private fun reading(member: String, level: Int, title: String = "کتاب") =
        BookReading("id-$member-$level-$title", member, title, 0L, level, 0L)

    @Test
    fun savesTrimmedTitleWithValidLevel() = runBlocking {
        val repo = RecordingReadingRepository()
        val result = RecordBookReading(repo)("m1", "  شازده کوچولو  ", 1_000L, 3, 2_000L)
        val saved = assertIs<RecordBookReadingResult.Saved>(result).reading
        assertEquals("شازده کوچولو", saved.title)
        assertEquals(3, saved.level)
        assertEquals(listOf(saved), repo.saved)
    }

    @Test
    fun rejectsBlankMemberTitleAndOutOfRangeLevel() = runBlocking {
        val repo = RecordingReadingRepository()
        val useCase = RecordBookReading(repo)
        assertEquals(RecordBookReadingResult.InvalidMember, useCase(" ", "کتاب", 0L, 3, 0L))
        assertEquals(RecordBookReadingResult.InvalidTitle, useCase("m1", "   ", 0L, 3, 0L))
        assertEquals(RecordBookReadingResult.InvalidLevel, useCase("m1", "کتاب", 0L, 0, 0L))
        assertEquals(RecordBookReadingResult.InvalidLevel, useCase("m1", "کتاب", 0L, 6, 0L))
        assertEquals(emptyList(), repo.saved)
    }

    @Test
    fun scoreAndRankingUseSummedLevels() {
        val rows = listOf(reading("a", 5), reading("a", 2, "دو"), reading("b", 4), reading("b", 3, "سه"), reading("c", 1))
        assertEquals(7L, ReadingScoreCalculator.score(rows, "a"))
        assertEquals(0L, ReadingScoreCalculator.score(rows, "zzz"))
        val ranking = ReadingScoreCalculator.ranking(rows)
        assertEquals(listOf("a", "b", "c"), ranking.map { it.memberId })
        assertEquals(2, ranking.first().bookCount)
    }
}

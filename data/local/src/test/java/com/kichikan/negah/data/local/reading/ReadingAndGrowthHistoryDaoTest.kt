package com.kichikan.negah.data.local.reading

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.growth.GrowthItemEntity
import com.kichikan.negah.data.local.growth.MemberGrowthProgressEntity
import com.kichikan.negah.data.local.member.MemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReadingAndGrowthHistoryDaoTest {
    private lateinit var database: NegahDatabase

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
        database.memberDao().insert(
            MemberEntity(id = MEMBER, fullName = "Test member", nickname = null, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L),
        )
        database.growthMapDao().upsertItem(
            GrowthItemEntity(ITEM, "CAPABILITY", "Item", null, 0, true, 1L, 1L),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readingsAreStoredNewestFirst() = runBlocking {
        val dao = database.bookReadingDao()
        dao.insert(BookReadingEntity("r1", MEMBER, "old", 100L, 2, 1L))
        dao.insert(BookReadingEntity("r2", MEMBER, "new", 200L, 5, 2L))
        assertEquals(listOf("r2", "r1"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun readingForUnknownMemberIsRejected() {
        val dao = database.bookReadingDao()
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(BookReadingEntity("r1", "missing", "x", 1L, 3, 1L)) }
        }
    }

    @Test
    fun progressChangesAreAppendedToHistoryOnlyWhenTheyChange() = runBlocking {
        val dao = database.growthMapDao()
        dao.saveProgressWithHistory(MemberGrowthProgressEntity(MEMBER, ITEM, 20, "start", 10L), "h1")
        dao.saveProgressWithHistory(MemberGrowthProgressEntity(MEMBER, ITEM, 20, "start", 20L), "h2")
        dao.saveProgressWithHistory(MemberGrowthProgressEntity(MEMBER, ITEM, 60, "better", 30L), "h3")

        val history = dao.observeHistory(MEMBER).first()
        assertEquals(listOf("h3", "h1"), history.map { it.id })
        assertNull(history.last().previousPercent)
        assertEquals(20, history.first().previousPercent)
        assertEquals(60, history.first().newPercent)
        assertEquals(60, dao.findProgress(MEMBER, ITEM)?.progressPercent)
    }

    private companion object {
        const val MEMBER = "member-1"
        const val ITEM = "item-1"
    }
}

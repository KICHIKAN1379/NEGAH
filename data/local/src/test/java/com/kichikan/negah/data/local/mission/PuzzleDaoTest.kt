package com.kichikan.negah.data.local.mission

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.group.MemberGroupEntity
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.mission.CompletePuzzlePieceResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PuzzleDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var dao: PuzzleDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), NegahDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = database.puzzleDao()
        database.memberDao().insert(MemberEntity(id = "m", fullName = "M", nickname = null, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L))
        database.groupDao().upsertGroup(MemberGroupEntity(id = "g", name = "G", leaderMemberId = "m", isActive = true, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L))
        database.missionDao().saveMission(MissionEntity("p", "GROUP_PUZZLE", "پازل", null, "POINTS", 30, true, 1L))
        database.missionDao().saveMission(MissionEntity("r", "REGULAR", "عادی", null, "XP", 5, true, 1L))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun sixthPieceRewardsGroupExactlyOnce() = runBlocking {
        for (i in 1..5) assertEquals(CompletePuzzlePieceResult.Completed(i, false), dao.completePiece("p", "g", i, 10L + i))
        assertEquals(CompletePuzzlePieceResult.AlreadyCompleted, dao.completePiece("p", "g", 5, 20L))
        assertEquals(0L, database.groupEconomyDao().balanceFor("g")!!.spendablePoints)
        assertEquals(CompletePuzzlePieceResult.Completed(6, true), dao.completePiece("p", "g", 6, 30L))
        assertEquals(30L, database.groupEconomyDao().balanceFor("g")!!.spendablePoints)
        assertEquals(1, database.groupEconomyDao().observeForGroup("g").first().size)
        assertEquals(6, dao.observePieces("p").first().size)
    }

    @Test
    fun invalidInputsChangeNothing() = runBlocking {
        assertEquals(CompletePuzzlePieceResult.InvalidPiece, dao.completePiece("p", "g", 7, 1L))
        assertEquals(CompletePuzzlePieceResult.InvalidPuzzleOrGroup, dao.completePiece("r", "g", 1, 1L))
        assertEquals(CompletePuzzlePieceResult.InvalidPuzzleOrGroup, dao.completePiece("p", "ghost", 1, 1L))
        assertEquals(0, dao.observePieces("p").first().size)
    }
}

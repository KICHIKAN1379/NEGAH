package com.kichikan.negah.data.local.group

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.group.GroupEconomyAppendResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GroupDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var dao: GroupDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.groupDao()
        listOf("a", "b", "c", "d").forEach { id ->
            database.memberDao().insert(
                MemberEntity(id = id, fullName = "Member $id", nickname = null, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun group(id: String, leader: String) =
        MemberGroupEntity(id, "Group $id", leader, true, 1L, 1L)


    @Test
    fun groupEconomyLedgerUpdatesIndependentBalancesAndRetainsRows() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b")))
        val economyDao = database.groupEconomyDao()
        val result = economyDao.appendAtomically(
            listOf(
                GroupEconomyTransactionEntity("points-1", "g1", "POINTS", 120L, "پاداش گروه", 10L),
                GroupEconomyTransactionEntity("diamonds-1", "g1", "DIAMONDS", 4L, "پاداش گروه", 10L),
            ),
        )

        assertEquals(GroupEconomyAppendResult.APPENDED, result)
        val saved = dao.observeGroups().first().single()
        assertEquals(120L, saved.spendablePoints)
        assertEquals(4L, saved.diamonds)
        assertEquals(2, economyDao.observeForGroup("g1").first().size)
    }

    @Test
    fun insufficientGroupBalanceDoesNotWriteLedgerOrChangeBalance() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b")))
        val economyDao = database.groupEconomyDao()
        val result = economyDao.appendAtomically(
            listOf(
                GroupEconomyTransactionEntity("points-negative", "g1", "POINTS", -1L, "اصلاح", 11L),
            ),
        )

        assertEquals(GroupEconomyAppendResult.INSUFFICIENT_BALANCE, result)
        assertEquals(0L, dao.observeGroups().first().single().spendablePoints)
        assertTrue(economyDao.observeForGroup("g1").first().isEmpty())
    }

    @Test
    fun mixedCurrencyAdjustmentIsAtomicWhenOneBalanceWouldBecomeNegative() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b")))
        val economyDao = database.groupEconomyDao()
        val result = economyDao.appendAtomically(
            listOf(
                GroupEconomyTransactionEntity("points-positive", "g1", "POINTS", 50L, "اصلاح", 12L),
                GroupEconomyTransactionEntity("diamonds-negative", "g1", "DIAMONDS", -1L, "اصلاح", 12L),
            ),
        )

        assertEquals(GroupEconomyAppendResult.INSUFFICIENT_BALANCE, result)
        val saved = dao.observeGroups().first().single()
        assertEquals(0L, saved.spendablePoints)
        assertEquals(0L, saved.diamonds)
        assertTrue(economyDao.observeForGroup("g1").first().isEmpty())
    }


    @Test
    fun editingGroupMetadataDoesNotResetIndependentEconomy() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b")))
        val economyDao = database.groupEconomyDao()
        assertEquals(
            GroupEconomyAppendResult.APPENDED,
            economyDao.appendAtomically(
                listOf(GroupEconomyTransactionEntity("points-keep", "g1", "POINTS", 75L, "پاداش", 13L)),
            ),
        )

        assertTrue(dao.saveGroup(group("g1", "a"), listOf("c")))
        val saved = dao.observeGroups().first().single()

        assertEquals(75L, saved.spendablePoints)
        assertEquals(0L, saved.diamonds)
        assertEquals(listOf("c"), dao.observeMemberships().first().map { it.memberId })
    }

    @Test
    fun savesGroupWithLeaderAndSubMembers() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b", "c")))
        assertEquals(listOf("g1"), dao.observeGroups().first().map { it.id })
        assertEquals(setOf("b", "c"), dao.observeMemberships().first().map { it.memberId }.toSet())
    }

    @Test
    fun memberCannotJoinASecondActiveGroupAsLeaderOrSubMember() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b")))
        assertFalse(dao.saveGroup(group("g2", "c"), listOf("b")))
        assertFalse(dao.saveGroup(group("g3", "a"), listOf("d")))
        assertEquals(listOf("g1"), dao.observeGroups().first().map { it.id })
    }

    @Test
    fun editingReplacesMembershipAndArchivingFreesMembers() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b", "c")))
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("d")))
        assertEquals(setOf("d"), dao.observeMemberships().first().map { it.memberId }.toSet())

        dao.deactivate("g1", 5L)
        assertTrue(dao.saveGroup(group("g2", "b"), listOf("a", "d")))
        assertEquals(false, dao.observeGroups().first().first { it.id == "g1" }.isActive)
    }
}

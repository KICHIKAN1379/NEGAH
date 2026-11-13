package com.kichikan.negah.data.local.wheel

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.wheel.WheelSpinResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WheelDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var dao: WheelDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), NegahDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = database.wheelDao()
        database.memberDao().insert(MemberEntity(id = "m", fullName = "Member", nickname = null, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L))
        dao.upsertSettings(WheelSettingsEntity(1, 50))
        dao.savePrize(WheelPrizeEntity("diamond", "الماس", "DIAMONDS", 2, 1, true, 1L, 1L))
    }

    @After
    fun tearDown() = database.close()

    private suspend fun givePoints(amount: Long) {
        database.economyTransactionDao().appendAtomically(
            EconomyTransactionEntity("seed:$amount", "m", "POINTS", amount, "EARN", "TEST", null, 2L),
        )
    }

    @Test
    fun spinDebitsCostCreditsRewardAndRecordsHistoryAtomically() = runBlocking {
        givePoints(80)
        val result = dao.spinAtomically("m", "s1", 10L) { 0L }
        assertTrue(result is WheelSpinResult.Won)
        val member = database.memberDao().observeActiveMembers().first().single()
        assertEquals(30L, member.spendablePoints)
        assertEquals(2L, member.diamonds)
        val spin = dao.observeSpins().first().single()
        assertEquals(50L, spin.costPoints)
        assertEquals("الماس", spin.prizeLabel)
    }

    @Test
    fun insufficientPointsWritesNothing() = runBlocking {
        givePoints(49)
        assertEquals(WheelSpinResult.InsufficientPoints, dao.spinAtomically("m", "s1", 10L) { 0L })
        val member = database.memberDao().observeActiveMembers().first().single()
        assertEquals(49L, member.spendablePoints)
        assertEquals(0L, member.diamonds)
        assertTrue(dao.observeSpins().first().isEmpty())
    }

    @Test
    fun noActivePrizesAndUnknownMemberAreRejected() = runBlocking {
        givePoints(100)
        assertEquals(WheelSpinResult.InvalidMember, dao.spinAtomically("ghost", "s1", 10L) { 0L })
        dao.savePrize(WheelPrizeEntity("diamond", "الماس", "DIAMONDS", 2, 1, false, 1L, 5L))
        assertEquals(WheelSpinResult.NoActivePrizes, dao.spinAtomically("m", "s2", 10L) { 0L })
        assertEquals(100L, database.memberDao().observeActiveMembers().first().single().spendablePoints)
    }

    @Test
    fun editingPrizeKeepsCreationTimeAndPastSpinLabel() = runBlocking {
        givePoints(60)
        dao.spinAtomically("m", "s1", 10L) { 0L }
        dao.savePrize(WheelPrizeEntity("diamond", "نام جدید", "DIAMONDS", 9, 3, true, 99L, 99L))
        assertEquals(1L, dao.observePrizes().first().single().createdAtEpochMillis)
        assertEquals("الماس", dao.observeSpins().first().single().prizeLabel)
    }
}

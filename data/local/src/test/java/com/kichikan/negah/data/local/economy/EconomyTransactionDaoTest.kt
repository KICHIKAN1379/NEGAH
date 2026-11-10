package com.kichikan.negah.data.local.economy

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.member.MemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EconomyTransactionDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var dao: EconomyTransactionDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.economyTransactionDao()
        database.memberDao().insert(
            MemberEntity(
                id = MEMBER_ID,
                fullName = "Test member",
                nickname = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun earningPoints_appendsLedgerAndRefreshesCachedBalance() = runBlocking {
        val result = dao.appendAtomically(transaction(id = "earn-1", amount = 40L))

        assertEquals(LedgerAppendResult.APPENDED, result)
        assertEquals(40L, dao.balanceFor(MEMBER_ID, "POINTS"))
        assertEquals(40L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertEquals(1, dao.observeForMember(MEMBER_ID).first().size)
    }

    @Test
    fun spendingWithinBalance_updatesLedgerAndCachedBalance() = runBlocking {
        dao.appendAtomically(transaction(id = "earn-1", amount = 40L))
        val result = dao.appendAtomically(
            transaction(id = "spend-1", amount = -15L, kind = "SPEND"),
        )

        assertEquals(LedgerAppendResult.APPENDED, result)
        assertEquals(25L, dao.balanceFor(MEMBER_ID, "POINTS"))
        assertEquals(25L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertEquals(2, dao.observeForMember(MEMBER_ID).first().size)
    }

    @Test
    fun insufficientBalance_writesNothingAndLeavesCachedBalanceUntouched() = runBlocking {
        dao.appendAtomically(transaction(id = "earn-1", amount = 12L))
        val result = dao.appendAtomically(
            transaction(id = "spend-1", amount = -20L, kind = "SPEND"),
        )

        assertEquals(LedgerAppendResult.INSUFFICIENT_BALANCE, result)
        assertEquals(12L, dao.balanceFor(MEMBER_ID, "POINTS"))
        assertEquals(12L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertEquals(1, dao.observeForMember(MEMBER_ID).first().size)
    }

    @Test
    fun negativeXpCorrection_isAppliedOnlyWhenBalanceRemainsNonNegative() = runBlocking {
        val earned = transaction(id = "xp-earn", amount = 70L, currency = "XP")
        val correction = transaction(
            id = "xp-correction",
            amount = -20L,
            currency = "XP",
            kind = "CORRECTION",
        )
        assertEquals(LedgerAppendResult.APPENDED, dao.appendAtomically(earned))
        assertEquals(LedgerAppendResult.APPENDED, dao.appendAtomically(correction))
        assertEquals(50L, dao.balanceFor(MEMBER_ID, "XP"))
        assertEquals(50L, database.memberDao().findById(MEMBER_ID)?.totalXp)

        val rejected = dao.appendAtomically(
            transaction(
                id = "xp-over-correction",
                amount = -51L,
                currency = "XP",
                kind = "CORRECTION",
            ),
        )
        assertEquals(LedgerAppendResult.INSUFFICIENT_BALANCE, rejected)
        assertEquals(50L, database.memberDao().findById(MEMBER_ID)?.totalXp)
        assertEquals(2, dao.observeForMember(MEMBER_ID).first().size)
    }

    @Test
    fun duplicateTransactionId_rollsBackWithoutChangingCachedBalance() = runBlocking {
        dao.appendAtomically(transaction(id = "same-id", amount = 30L))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                dao.appendAtomically(transaction(id = "same-id", amount = 10L))
            }
        }

        assertEquals(30L, dao.balanceFor(MEMBER_ID, "POINTS"))
        assertEquals(30L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertEquals(1, dao.observeForMember(MEMBER_ID).first().size)
    }

    @Test
    fun transactionHistory_hasStableOrderingWhenTimestampsMatch() = runBlocking {
        dao.appendAtomically(transaction(id = "a", amount = 1L, createdAt = 10L))
        dao.appendAtomically(transaction(id = "z", amount = 1L, createdAt = 10L))

        val ids = dao.observeForMember(MEMBER_ID).first().map { it.id }
        assertEquals(listOf("z", "a"), ids)
    }

    private fun transaction(
        id: String,
        amount: Long,
        currency: String = "POINTS",
        kind: String = "EARN",
        createdAt: Long = 100L,
    ) = EconomyTransactionEntity(
        id = id,
        memberId = MEMBER_ID,
        currency = currency,
        amount = amount,
        transactionType = kind,
        sourceType = "TEST",
        sourceId = null,
        createdAtEpochMillis = createdAt,
    )

    private companion object {
        const val MEMBER_ID = "member-1"
    }
}

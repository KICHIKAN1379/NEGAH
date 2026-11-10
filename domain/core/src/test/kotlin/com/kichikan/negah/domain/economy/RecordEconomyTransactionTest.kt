package com.kichikan.negah.domain.economy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordEconomyTransactionTest {
    private class FakeEconomyRepository(
        var result: LedgerAppendResult = LedgerAppendResult.APPENDED,
    ) : EconomyRepository {
        val appended = mutableListOf<EconomyTransaction>()
        override fun observeForMember(memberId: String): Flow<List<EconomyTransaction>> = emptyFlow()
        override suspend fun appendAtomically(transaction: EconomyTransaction): LedgerAppendResult {
            appended += transaction
            return result
        }
    }

    private fun transaction(
        currency: EconomyCurrency = EconomyCurrency.POINTS,
        delta: Long = 10L,
        kind: EconomyTransactionKind = EconomyTransactionKind.EARN,
    ) = EconomyTransaction(
        id = "tx-1",
        memberId = "member-1",
        currency = currency,
        amountDelta = delta,
        kind = kind,
        sourceType = "coach_adjustment",
        sourceId = null,
        createdAtEpochMillis = 100L,
    )

    @Test
    fun xpCannotBeSpent() = runBlocking {
        val repository = FakeEconomyRepository()
        val result = RecordEconomyTransaction(repository)(
            transaction(EconomyCurrency.XP, -5L, EconomyTransactionKind.SPEND),
        )
        assertEquals(RecordEconomyTransactionResult.InvalidTransaction, result)
        assertEquals(0, repository.appended.size)
    }

    @Test
    fun pointAndDiamondSpendingAreAllowed() = runBlocking {
        val repository = FakeEconomyRepository()
        val useCase = RecordEconomyTransaction(repository)
        assertEquals(
            RecordEconomyTransactionResult.Appended,
            useCase(transaction(EconomyCurrency.POINTS, -5L, EconomyTransactionKind.SPEND)),
        )
        assertEquals(
            RecordEconomyTransactionResult.Appended,
            useCase(transaction(EconomyCurrency.DIAMONDS, -2L, EconomyTransactionKind.SPEND)),
        )
    }

    @Test
    fun correctionCanBePositiveOrNegative() = runBlocking {
        val repository = FakeEconomyRepository()
        val useCase = RecordEconomyTransaction(repository)
        assertEquals(
            RecordEconomyTransactionResult.Appended,
            useCase(transaction(EconomyCurrency.XP, -3L, EconomyTransactionKind.CORRECTION)),
        )
        assertEquals(
            RecordEconomyTransactionResult.Appended,
            useCase(transaction(EconomyCurrency.XP, 3L, EconomyTransactionKind.CORRECTION)),
        )
    }

    @Test
    fun invalidAmountsAreRejectedBeforePersistence() = runBlocking {
        val repository = FakeEconomyRepository()
        val result = RecordEconomyTransaction(repository)(
            transaction(EconomyCurrency.POINTS, 0L, EconomyTransactionKind.EARN),
        )
        assertEquals(RecordEconomyTransactionResult.InvalidTransaction, result)
        assertEquals(0, repository.appended.size)
    }

    @Test
    fun repositoryBalanceFailuresArePropagated() = runBlocking {
        val insufficient = RecordEconomyTransaction(FakeEconomyRepository(LedgerAppendResult.INSUFFICIENT_BALANCE))
        val overflow = RecordEconomyTransaction(FakeEconomyRepository(LedgerAppendResult.BALANCE_OVERFLOW))
        assertEquals(
            RecordEconomyTransactionResult.InsufficientBalance,
            insufficient(transaction(EconomyCurrency.POINTS, -500L, EconomyTransactionKind.SPEND)),
        )
        assertEquals(
            RecordEconomyTransactionResult.BalanceOverflow,
            overflow(transaction()),
        )
    }
}

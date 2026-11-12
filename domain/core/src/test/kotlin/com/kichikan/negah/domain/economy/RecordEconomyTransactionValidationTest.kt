package com.kichikan.negah.domain.economy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordEconomyTransactionValidationTest {
    private class RecordingRepository : EconomyRepository {
        var calls = 0
        override fun observeForMember(memberId: String): Flow<List<EconomyTransaction>> = emptyFlow()
        override suspend fun appendAtomically(transaction: EconomyTransaction): LedgerAppendResult {
            calls++
            return LedgerAppendResult.APPENDED
        }
    }

    private fun tx(
        currency: EconomyCurrency = EconomyCurrency.POINTS,
        delta: Long = 1,
        kind: EconomyTransactionKind = EconomyTransactionKind.EARN,
        sourceType: String = "test",
    ) = EconomyTransaction("id", "member", currency, delta, kind, sourceType, null, 1L)

    @Test fun earnMustBePositive() = runBlocking {
        val repo = RecordingRepository()
        assertEquals(
            RecordEconomyTransactionResult.InvalidTransaction,
            RecordEconomyTransaction(repo)(tx(delta = -1)),
        )
        assertEquals(0, repo.calls)
    }

    @Test fun refundMustBePositiveAndCannotRefundXp() = runBlocking {
        val repo = RecordingRepository()
        val useCase = RecordEconomyTransaction(repo)
        assertEquals(
            RecordEconomyTransactionResult.InvalidTransaction,
            useCase(tx(currency = EconomyCurrency.XP, kind = EconomyTransactionKind.REFUND)),
        )
        assertEquals(
            RecordEconomyTransactionResult.InvalidTransaction,
            useCase(tx(delta = -1, kind = EconomyTransactionKind.REFUND)),
        )
        assertEquals(0, repo.calls)
    }

    @Test fun requiredIdentityFieldsCannotBeBlank() = runBlocking {
        val repo = RecordingRepository()
        assertEquals(
            RecordEconomyTransactionResult.InvalidTransaction,
            RecordEconomyTransaction(repo)(tx(sourceType = " ")),
        )
        assertEquals(0, repo.calls)
    }
}

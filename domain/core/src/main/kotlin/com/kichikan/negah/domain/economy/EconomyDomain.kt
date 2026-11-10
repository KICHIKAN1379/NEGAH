package com.kichikan.negah.domain.economy

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class EconomyCurrency { XP, POINTS, DIAMONDS }
enum class EconomyTransactionKind { EARN, SPEND, CORRECTION, REFUND }

data class EconomyTransaction(
    val id: String,
    val memberId: String,
    val currency: EconomyCurrency,
    /** Signed delta: positive adds to a balance; negative spends or corrects it. */
    val amountDelta: Long,
    val kind: EconomyTransactionKind,
    val sourceType: String,
    val sourceId: String?,
    val createdAtEpochMillis: Long,
)

enum class LedgerAppendResult { APPENDED, INSUFFICIENT_BALANCE, BALANCE_OVERFLOW }

interface EconomyRepository {
    fun observeForMember(memberId: String): Flow<List<EconomyTransaction>>
    suspend fun appendAtomically(transaction: EconomyTransaction): LedgerAppendResult
}

sealed interface RecordEconomyTransactionResult {
    data object InvalidTransaction : RecordEconomyTransactionResult
    data object Appended : RecordEconomyTransactionResult
    data object InsufficientBalance : RecordEconomyTransactionResult
    data object BalanceOverflow : RecordEconomyTransactionResult
}

class RecordEconomyTransaction @Inject constructor(
    private val repository: EconomyRepository,
) {
    suspend operator fun invoke(transaction: EconomyTransaction): RecordEconomyTransactionResult {
        if (transaction.id.isBlank() || transaction.memberId.isBlank() ||
            transaction.sourceType.isBlank() || transaction.amountDelta == 0L
        ) return RecordEconomyTransactionResult.InvalidTransaction

        val validKind = when (transaction.kind) {
            EconomyTransactionKind.EARN -> transaction.amountDelta > 0
            EconomyTransactionKind.SPEND ->
                transaction.amountDelta < 0 && transaction.currency != EconomyCurrency.XP
            EconomyTransactionKind.CORRECTION -> true
            EconomyTransactionKind.REFUND ->
                transaction.amountDelta > 0 && transaction.currency != EconomyCurrency.XP
        }
        if (!validKind) return RecordEconomyTransactionResult.InvalidTransaction

        return when (repository.appendAtomically(transaction)) {
            LedgerAppendResult.APPENDED -> RecordEconomyTransactionResult.Appended
            LedgerAppendResult.INSUFFICIENT_BALANCE -> RecordEconomyTransactionResult.InsufficientBalance
            LedgerAppendResult.BALANCE_OVERFLOW -> RecordEconomyTransactionResult.BalanceOverflow
        }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

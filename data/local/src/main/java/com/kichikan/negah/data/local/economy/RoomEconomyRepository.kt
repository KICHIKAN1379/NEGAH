package com.kichikan.negah.data.local.economy

import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyRepository
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.economy.EconomyTransactionKind
import com.kichikan.negah.domain.economy.LedgerAppendResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEconomyRepository @Inject constructor(
    private val dao: EconomyTransactionDao,
) : EconomyRepository {
    override fun observeForMember(memberId: String): Flow<List<EconomyTransaction>> =
        dao.observeForMember(memberId).map { transactions -> transactions.map(EconomyTransactionEntity::toDomain) }

    override suspend fun appendAtomically(transaction: EconomyTransaction): LedgerAppendResult =
        dao.appendAtomically(EconomyTransactionEntity.fromDomain(transaction))
}

private fun EconomyTransactionEntity.toDomain() = EconomyTransaction(
    id = id,
    memberId = memberId,
    currency = EconomyCurrency.valueOf(currency),
    amountDelta = amount,
    kind = EconomyTransactionKind.valueOf(transactionType),
    sourceType = sourceType,
    sourceId = sourceId,
    createdAtEpochMillis = createdAtEpochMillis,
)

private fun EconomyTransactionEntity.Companion.fromDomain(transaction: EconomyTransaction) =
    EconomyTransactionEntity(
        id = transaction.id,
        memberId = transaction.memberId,
        currency = transaction.currency.name,
        amount = transaction.amountDelta,
        transactionType = transaction.kind.name,
        sourceType = transaction.sourceType,
        sourceId = transaction.sourceId,
        createdAtEpochMillis = transaction.createdAtEpochMillis,
    )

package com.kichikan.negah.data.local.settings

import com.kichikan.negah.domain.settings.DailyQuote
import com.kichikan.negah.domain.settings.DailyQuoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDailyQuoteRepository @Inject constructor(private val dao: DailyQuoteDao) : DailyQuoteRepository {
    override fun observeAll(): Flow<List<DailyQuote>> = dao.observeAll().map { rows -> rows.map { DailyQuote(it.id, it.text, it.isActive, it.createdAtEpochMillis) } }
    override suspend fun save(quote: DailyQuote) {
        require(quote.id.isNotBlank() && quote.text.isNotBlank())
        dao.save(DailyQuoteEntity(quote.id, quote.text.trim(), quote.isActive, quote.createdAtEpochMillis))
    }
    override suspend fun delete(id: String) = dao.delete(id)
}
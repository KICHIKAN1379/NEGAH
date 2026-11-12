package com.kichikan.negah.domain.settings

import kotlinx.coroutines.flow.Flow

data class DailyQuote(val id: String, val text: String, val isActive: Boolean, val createdAtEpochMillis: Long)

interface DailyQuoteRepository {
    fun observeAll(): Flow<List<DailyQuote>>
    suspend fun save(quote: DailyQuote)
    suspend fun delete(id: String)
}
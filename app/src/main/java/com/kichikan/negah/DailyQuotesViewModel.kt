package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.settings.DailyQuote
import com.kichikan.negah.domain.settings.DailyQuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DailyQuotesUiState(val quotes: List<DailyQuote> = emptyList(), val draft: String = "", val editingId: String? = null, val isLoading: Boolean = true, val message: String? = null, val error: String? = null)

@HiltViewModel
class DailyQuotesViewModel @Inject constructor(private val repository: DailyQuoteRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DailyQuotesUiState())
    val state = mutableState.asStateFlow()
    init { viewModelScope.launch { repository.observeAll().collect { rows -> mutableState.update { it.copy(quotes = rows, isLoading = false) } } } }
    fun setDraft(value: String) { mutableState.update { it.copy(draft = value, error = null, message = null) } }
    fun edit(quote: DailyQuote) { mutableState.update { it.copy(draft = quote.text, editingId = quote.id, error = null, message = null) } }
    fun cancelEdit() { mutableState.update { it.copy(draft = "", editingId = null, error = null, message = null) } }
    fun toggle(quote: DailyQuote, active: Boolean) { viewModelScope.launch { runCatching { repository.save(quote.copy(isActive = active)) }.onFailure { mutableState.update { it.copy(error = "تغییر وضعیت جمله ذخیره نشد") } } } }
    fun delete(quote: DailyQuote) { viewModelScope.launch { runCatching { repository.delete(quote.id) }.onSuccess { mutableState.update { it.copy(message = "جمله حذف شد", error = null) } }.onFailure { mutableState.update { it.copy(error = "حذف جمله انجام نشد") } } } }
    fun save() {
        val current = mutableState.value
        val text = current.draft.trim()
        if (text.isBlank()) { mutableState.update { it.copy(error = "متن جمله را وارد کنید") }; return }
        val old = current.quotes.firstOrNull { it.id == current.editingId }
        val quote = DailyQuote(old?.id ?: UUID.randomUUID().toString(), text, old?.isActive ?: true, old?.createdAtEpochMillis ?: System.currentTimeMillis())
        viewModelScope.launch { runCatching { repository.save(quote) }.onSuccess { mutableState.update { it.copy(draft = "", editingId = null, message = "جمله ذخیره شد", error = null) } }.onFailure { mutableState.update { it.copy(error = "ذخیره جمله انجام نشد") } } }
    }
}
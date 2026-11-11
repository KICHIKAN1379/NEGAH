package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.settings.HomeShortcut
import com.kichikan.negah.domain.settings.HomeShortcutsRepository
import com.kichikan.negah.domain.settings.SaveHomeShortcuts
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeShortcutsUiState(
    val selected: List<HomeShortcut> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

@HiltViewModel
class HomeShortcutsViewModel @Inject constructor(
    repository: HomeShortcutsRepository,
    private val saveHomeShortcuts: SaveHomeShortcuts,
) : ViewModel() {
    private var persistedSelection: List<HomeShortcut> = emptyList()
    private val _uiState = MutableStateFlow(HomeShortcutsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSelectedShortcuts().collect { selected ->
                persistedSelection = selected
                _uiState.update {
                    it.copy(
                        selected = selected,
                        isLoading = false,
                        hasUnsavedChanges = false,
                    )
                }
            }
        }
    }

    fun toggle(shortcut: HomeShortcut) {
        _uiState.update { state ->
            if (state.isLoading || state.isSaving) return@update state
            val next = if (shortcut in state.selected) {
                state.selected - shortcut
            } else {
                if (state.selected.size >= HomeShortcut.MAX_SELECTION) {
                    return@update state.copy(
                        message = "حداکثر چهار میان‌بر را می‌توانید انتخاب کنید.",
                        messageIsError = true,
                    )
                }
                state.selected + shortcut
            }
            state.copy(
                selected = next,
                hasUnsavedChanges = true,
                message = null,
                messageIsError = false,
            )
        }
    }

    fun discardChanges() {
        _uiState.update { state ->
            if (state.isSaving) state else state.copy(
                selected = persistedSelection,
                hasUnsavedChanges = false,
                message = null,
                messageIsError = false,
            )
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || !current.hasUnsavedChanges) return
        val selected = current.selected
        _uiState.update { it.copy(isSaving = true, message = null, messageIsError = false) }

        viewModelScope.launch {
            try {
                saveHomeShortcuts(selected)
                persistedSelection = selected
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        message = "میان‌برهای خانه ذخیره شد.",
                        messageIsError = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = true,
                        message = "ذخیره میان‌برها ناموفق بود. دوباره تلاش کنید.",
                        messageIsError = true,
                    )
                }
            }
        }
    }
}

package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.settings.ClassDay
import com.kichikan.negah.domain.settings.ClassDaysRepository
import com.kichikan.negah.domain.settings.SaveClassDays
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassDaysUiState(
    val selectedDays: Set<ClassDay> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

@HiltViewModel
class ClassDaysViewModel @Inject constructor(
    private val repository: ClassDaysRepository,
    private val saveClassDays: SaveClassDays,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClassDaysUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSelectedDays().collect { days ->
                _uiState.update {
                    it.copy(
                        selectedDays = days,
                        isLoading = false,
                        hasUnsavedChanges = false,
                    )
                }
            }
        }
    }

    fun toggle(day: ClassDay) {
        _uiState.update { state ->
            if (state.isLoading || state.isSaving) return@update state
            val nextDays = if (day in state.selectedDays) {
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }
            state.copy(
                selectedDays = nextDays,
                hasUnsavedChanges = true,
                message = null,
                messageIsError = false,
            )
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || !current.hasUnsavedChanges) return
        val selectedDays = current.selectedDays
        _uiState.update { it.copy(isSaving = true, message = null, messageIsError = false) }

        viewModelScope.launch {
            try {
                saveClassDays(selectedDays)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        message = "روزهای کلاس ذخیره شد.",
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
                        message = "ذخیره روزهای کلاس ناموفق بود. دوباره تلاش کنید.",
                        messageIsError = true,
                    )
                }
            }
        }
    }
}

package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.settings.RingProfile
import com.kichikan.negah.domain.settings.RingProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingProfileUiState(
    val profile: RingProfile = RingProfile(),
    val nameDraft: String = "",
    val usernameDraft: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val hasChanges: Boolean
        get() = nameDraft != profile.name || usernameDraft != profile.username.orEmpty()
}

@HiltViewModel
class RingProfileViewModel @Inject constructor(
    private val repository: RingProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RingProfileUiState())
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeProfile().collect { profile ->
                mutableState.update { current ->
                    current.copy(
                        profile = profile,
                        nameDraft = profile.name,
                        usernameDraft = profile.username.orEmpty(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        mutableState.update { it.copy(nameDraft = value, message = null, error = null) }
    }

    fun onUsernameChanged(value: String) {
        mutableState.update { it.copy(usernameDraft = value, message = null, error = null) }
    }

    fun save() {
        val current = mutableState.value
        val name = current.nameDraft.trim()
        if (name.isBlank()) {
            mutableState.update { it.copy(error = "نام حلقه را وارد کنید", message = null) }
            return
        }
        if (current.isSaving || current.isLoading) return
        val profile = current.profile.copy(
            name = name,
            username = current.usernameDraft.trim().ifEmpty { null },
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, error = null, message = null) }
            runCatching { repository.saveProfile(profile) }
                .onSuccess {
                    mutableState.update { it.copy(profile = profile, nameDraft = profile.name,
                        usernameDraft = profile.username.orEmpty(), isSaving = false,
                        message = "پروفایل حلقه ذخیره شد") }
                }
                .onFailure {
                    mutableState.update { it.copy(isSaving = false, error = "ذخیره پروفایل انجام نشد") }
                }
        }
    }
}

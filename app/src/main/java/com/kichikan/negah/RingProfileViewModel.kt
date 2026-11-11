package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.settings.RingProfile
import com.kichikan.negah.domain.settings.RingProfileRepository
import com.kichikan.negah.domain.settings.RingProfileMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingProfileUiState(
    val profile: RingProfile = RingProfile(), val nameDraft: String = "",
    val usernameDraft: String = "", val ringImageBytes: ByteArray? = null,
    val bannerBytes: ByteArray? = null, val isLoading: Boolean = true,
    val isSaving: Boolean = false, val mediaMessage: String? = null,
    val message: String? = null, val error: String? = null,
) {
    val hasChanges get() = nameDraft != profile.name || usernameDraft != profile.username.orEmpty()
}

@HiltViewModel
class RingProfileViewModel @Inject constructor(
    private val repository: RingProfileRepository,
    private val mediaRepository: RingProfileMediaRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RingProfileUiState())
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeProfile().collect { profile ->
                mutableState.update { it.copy(profile=profile, nameDraft=profile.name,
                    usernameDraft=profile.username.orEmpty(), isLoading=false) }
            }
        }
        viewModelScope.launch {
            runCatching { mediaRepository.loadRingImage() to mediaRepository.loadRingBanner() }
                .onSuccess { (image,banner) -> mutableState.update { it.copy(ringImageBytes=image,bannerBytes=banner) } }
                .onFailure { mutableState.update { it.copy(mediaMessage="بارگذاری تصاویر پروفایل حلقه انجام نشد") } }
        }
    }

    fun onNameChanged(value: String) { mutableState.update { it.copy(nameDraft=value,message=null,error=null) } }
    fun onUsernameChanged(value: String) { mutableState.update { it.copy(usernameDraft=value,message=null,error=null) } }

    fun save() {
        val current=mutableState.value
        val name=current.nameDraft.trim()
        if(name.isBlank()){ mutableState.update { it.copy(error="نام حلقه را وارد کنید",message=null) }; return }
        if(current.isSaving||current.isLoading)return
        val profile=current.profile.copy(name=name,username=current.usernameDraft.trim().ifEmpty{null},
            updatedAtEpochMillis=System.currentTimeMillis())
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving=true,error=null,message=null) }
            runCatching { repository.saveProfile(profile) }
                .onSuccess { mutableState.update { it.copy(profile=profile,nameDraft=profile.name,
                    usernameDraft=profile.username.orEmpty(),isSaving=false,message="پروفایل حلقه ذخیره شد") } }
                .onFailure { mutableState.update { it.copy(isSaving=false,error="ذخیره پروفایل انجام نشد") } }
        }
    }

    fun saveRingImage(input: InputStream,mimeType: String)=saveMedia(input,mimeType,true)
    fun saveRingBanner(input: InputStream,mimeType: String)=saveMedia(input,mimeType,false)
    private fun saveMedia(input: InputStream,mimeType: String,isImage: Boolean) {
        viewModelScope.launch {
            runCatching {
                if(isImage) mediaRepository.saveRingImage(input,mimeType) else mediaRepository.saveRingBanner(input,mimeType)
                if(isImage) mediaRepository.loadRingImage() else mediaRepository.loadRingBanner()
            }.onSuccess { bytes -> mutableState.update {
                if(isImage) it.copy(ringImageBytes=bytes,mediaMessage="تصویر حلقه ذخیره شد")
                else it.copy(bannerBytes=bytes,mediaMessage="بنر حلقه ذخیره شد")
            } }.onFailure { input.close(); mutableState.update { it.copy(mediaMessage="ذخیره تصویر انجام نشد") } }
        }
    }
    fun removeRingImage()=removeMedia(true)
    fun removeRingBanner()=removeMedia(false)
    private fun removeMedia(isImage: Boolean) {
        viewModelScope.launch {
            runCatching { if(isImage) mediaRepository.removeRingImage() else mediaRepository.removeRingBanner() }
                .onSuccess { mutableState.update {
                    if(isImage) it.copy(ringImageBytes=null,mediaMessage="تصویر حلقه حذف شد")
                    else it.copy(bannerBytes=null,mediaMessage="بنر حلقه حذف شد")
                } }.onFailure { mutableState.update { it.copy(mediaMessage="حذف تصویر انجام نشد") } }
        }
    }
}

package com.kichikan.negah

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BackgroundMusicState(
    val selectedUri: String? = null,
    val displayName: String? = null,
    val enabled: Boolean = false,
    val playing: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class BackgroundMusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val audioManager = requireNotNull(context.getSystemService(AudioManager::class.java))
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focusPolicy = AudioFocusPolicy()
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val mutableState = MutableStateFlow(
        BackgroundMusicState(
            selectedUri = preferences.getString(KEY_URI, null),
            displayName = preferences.getString(KEY_NAME, null),
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            message = if (preferences.getString(KEY_URI, null) == null) {
                "هنوز فایل موسیقی انتخاب نشده است."
            } else null,
        ),
    )
    val state = mutableState.asStateFlow()

    private var player: MediaPlayer? = null
    private var prepared = false
    private var isForeground = true

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener(::onAudioFocusChanged)

    fun selectAudio(uri: Uri?) {
        if (uri == null) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.onFailure {
            mutableState.update { state -> state.copy(message = "دسترسی پایدار به فایل صوتی برقرار نشد.") }
            return
        }

        pausePlayback(abandonFocus = true)
        releasePlayer()
        val name = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: "فایل موسیقی انتخاب‌شده"
        preferences.edit().putString(KEY_URI, uri.toString()).putString(KEY_NAME, name).apply()
        mutableState.update {
            it.copy(selectedUri = uri.toString(), displayName = name, message = "فایل موسیقی انتخاب شد.")
        }
        if (mutableState.value.enabled) startPlayback()
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled && mutableState.value.selectedUri == null) {
            mutableState.update { it.copy(enabled = false, message = "ابتدا فایل موسیقی را انتخاب کنید.") }
            preferences.edit().putBoolean(KEY_ENABLED, false).apply()
            return
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        mutableState.update { it.copy(enabled = enabled, message = null) }
        if (enabled) {
            if (isForeground) startPlayback()
        } else {
            pausePlayback(abandonFocus = true)
        }
    }

    fun onAppForegrounded() {
        isForeground = true
        if (mutableState.value.enabled) startPlayback()
    }

    fun onAppBackgrounded() {
        isForeground = false
        pausePlayback(abandonFocus = true)
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .setAcceptsDelayedFocusGain(false)
            .build()
        focusRequest = request
        return if (audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasAudioFocus = true
            true
        } else {
            focusRequest = null
            false
        }
    }

    private fun onAudioFocusChanged(change: Int) {
        if (focusRequest == null) return
        val event = when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> AudioFocusChange.GAIN
            AudioManager.AUDIOFOCUS_LOSS -> AudioFocusChange.PERMANENT_LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusChange.TRANSIENT_LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocusChange.TRANSIENT_DUCK
            else -> AudioFocusChange.OTHER
        }
        when (event) {
            AudioFocusChange.GAIN -> hasAudioFocus = true
            AudioFocusChange.TRANSIENT_LOSS, AudioFocusChange.PERMANENT_LOSS -> hasAudioFocus = false
            else -> Unit
        }

        when (focusPolicy.onFocusChange(event)) {
            AudioFocusAction.PAUSE -> {
                pausePlayerOnly()
                mutableState.update {
                    it.copy(
                        playing = false,
                        message = if (event == AudioFocusChange.PERMANENT_LOSS) {
                            "پخش موسیقی به‌دلیل استفادهٔ برنامه‌ای دیگر از صدا متوقف شد."
                        } else {
                            "موسیقی موقتاً مکث شد و پس از آزاد شدن صدا ادامه می‌یابد."
                        },
                    )
                }
                if (event == AudioFocusChange.PERMANENT_LOSS) abandonAudioFocus()
            }
            AudioFocusAction.DUCK -> {
                if (prepared) runCatching { player?.setVolume(DUCK_VOLUME, DUCK_VOLUME) }
            }
            AudioFocusAction.RESUME -> {
                if (isForeground && mutableState.value.enabled) {
                    startPreparedPlayer()
                } else {
                    focusPolicy.reset()
                }
            }
            AudioFocusAction.RESTORE_VOLUME -> restoreVolume()
            AudioFocusAction.NONE -> Unit
        }
    }

    private fun startPlayback() {
        val selected = mutableState.value.selectedUri ?: return
        if (!isForeground || !mutableState.value.enabled) return
        if (!requestAudioFocus()) {
            mutableState.update {
                it.copy(playing = false, message = "در حال حاضر صدای برنامهٔ دیگری در اولویت است؛ بعداً دوباره تلاش کنید.")
            }
            return
        }
        if (player != null) {
            if (prepared && player?.isPlaying == false) startPreparedPlayer()
            return
        }
        try {
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            prepared = false
            mediaPlayer.setAudioAttributes(audioAttributes)
            mediaPlayer.isLooping = true
            mediaPlayer.setOnPreparedListener {
                if (player !== it) return@setOnPreparedListener
                prepared = true
                if (isForeground && mutableState.value.enabled && hasAudioFocus) {
                    startPreparedPlayer()
                }
            }
            mediaPlayer.setOnErrorListener { failedPlayer, _, _ ->
                failedPlayer.release()
                if (player !== failedPlayer) return@setOnErrorListener true
                player = null
                prepared = false
                abandonAudioFocus()
                mutableState.update {
                    it.copy(playing = false, message = "پخش این فایل ممکن نیست؛ فایل صوتی دیگری انتخاب کنید.")
                }
                true
            }
            mediaPlayer.setDataSource(context, Uri.parse(selected))
            mediaPlayer.prepareAsync()
            mutableState.update { it.copy(playing = false, message = "در حال آماده‌سازی موسیقی…") }
        } catch (failure: Exception) {
            releasePlayer()
            abandonAudioFocus()
            mutableState.update {
                it.copy(playing = false, message = "فایل صوتی قابل پخش نیست؛ فایل دیگری انتخاب کنید.")
            }
        }
    }

    private fun startPreparedPlayer() {
        val activePlayer = player ?: return
        if (!prepared || !hasAudioFocus || !isForeground || !mutableState.value.enabled) return
        runCatching {
            val volume = if (focusPolicy.isDucked) DUCK_VOLUME else NORMAL_VOLUME
            activePlayer.setVolume(volume, volume)
            if (!activePlayer.isPlaying) activePlayer.start()
        }.onSuccess {
            mutableState.update { it.copy(playing = true, message = null) }
        }.onFailure {
            showPlaybackError()
        }
    }

    private fun restoreVolume() {
        if (prepared) runCatching { player?.setVolume(NORMAL_VOLUME, NORMAL_VOLUME) }
    }

    private fun pausePlayerOnly() {
        if (prepared) runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    private fun pausePlayback(abandonFocus: Boolean = false) {
        pausePlayerOnly()
        if (abandonFocus) abandonAudioFocus()
        mutableState.update { it.copy(playing = false) }
    }

    private fun showPlaybackError() {
        releasePlayer()
        abandonAudioFocus()
        mutableState.update {
            it.copy(playing = false, message = "پخش موسیقی متوقف شد؛ فایل را دوباره انتخاب کنید.")
        }
    }

    private fun releasePlayer() {
        player?.let { runCatching { it.release() } }
        player = null
        prepared = false
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
        hasAudioFocus = false
        focusPolicy.reset()
    }

    override fun onCleared() {
        pausePlayerOnly()
        releasePlayer()
        abandonAudioFocus()
        super.onCleared()
    }

    private companion object {
        const val PREFERENCES = "background_music"
        const val KEY_URI = "audio_uri"
        const val KEY_NAME = "audio_name"
        const val KEY_ENABLED = "enabled"
        const val DUCK_VOLUME = 0.2f
        const val NORMAL_VOLUME = 1f
    }
}

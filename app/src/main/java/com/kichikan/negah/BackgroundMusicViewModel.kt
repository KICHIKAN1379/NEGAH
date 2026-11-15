package com.kichikan.negah

import android.content.Context
import android.media.AudioAttributes
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

    fun selectAudio(uri: Uri?) {
        if (uri == null) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure {
            mutableState.update { state -> state.copy(message = "دسترسی پایدار به فایل صوتی برقرار نشد.") }
            return
        }
        // Selecting a new track must release the previous decoder before
        // starting playback from the newly persisted URI.
        releasePlayer()
        mutableState.update { it.copy(playing = false) }
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: "فایل موسیقی انتخاب‌شده"
        preferences.edit().putString(KEY_URI, uri.toString()).putString(KEY_NAME, name).apply()
        mutableState.update { it.copy(selectedUri = uri.toString(), displayName = name, message = "فایل موسیقی انتخاب شد.") }
        if (mutableState.value.enabled) startPlayback()
    }

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        mutableState.update { it.copy(enabled = enabled, message = null) }
        if (enabled) {
            if (mutableState.value.selectedUri == null) {
                mutableState.update { it.copy(enabled = false, message = "ابتدا فایل موسیقی را انتخاب کنید.") }
                preferences.edit().putBoolean(KEY_ENABLED, false).apply()
            } else if (isForeground) {
                startPlayback()
            }
        } else {
            pausePlayback()
        }
    }

    fun onAppForegrounded() {
        isForeground = true
        if (mutableState.value.enabled) startPlayback()
    }

    fun onAppBackgrounded() {
        isForeground = false
        pausePlayback()
    }

    private fun startPlayback() {
        val selected = mutableState.value.selectedUri ?: return
        if (!isForeground || !mutableState.value.enabled) return
        if (player != null) {
            if (prepared && player?.isPlaying == false) {
                runCatching { player?.start() }
                    .onSuccess { mutableState.update { it.copy(playing = true, message = null) } }
                    .onFailure { showPlaybackError() }
            }
            return
        }
        try {
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            prepared = false
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mediaPlayer.isLooping = true
            mediaPlayer.setOnPreparedListener {
                prepared = true
                if (isForeground && mutableState.value.enabled) {
                    runCatching { it.start() }
                        .onSuccess { mutableState.update { state -> state.copy(playing = true, message = null) } }
                        .onFailure { showPlaybackError() }
                }
            }
            mediaPlayer.setOnErrorListener { failedPlayer, _, _ ->
                failedPlayer.release()
                if (player === failedPlayer) player = null
                prepared = false
                mutableState.update { it.copy(playing = false, message = "پخش این فایل ممکن نیست؛ فایل صوتی دیگری انتخاب کنید.") }
                true
            }
            mediaPlayer.setDataSource(context, Uri.parse(selected))
            mediaPlayer.prepareAsync()
            mutableState.update { it.copy(playing = false, message = "در حال آماده‌سازی موسیقی…") }
        } catch (failure: Exception) {
            releasePlayer()
            mutableState.update { it.copy(playing = false, message = "فایل صوتی قابل پخش نیست؛ فایل دیگری انتخاب کنید.") }
        }
    }

    private fun pausePlayback() {
        if (prepared) runCatching { if (player?.isPlaying == true) player?.pause() }
        mutableState.update { it.copy(playing = false) }
    }

    private fun showPlaybackError() {
        releasePlayer()
        mutableState.update { it.copy(playing = false, message = "پخش موسیقی متوقف شد؛ فایل را دوباره انتخاب کنید.") }
    }

    private fun releasePlayer() {
        player?.let { runCatching { it.release() } }
        player = null
        prepared = false
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }

    private companion object {
        const val PREFERENCES = "background_music"
        const val KEY_URI = "audio_uri"
        const val KEY_NAME = "audio_name"
        const val KEY_ENABLED = "enabled"
    }
}

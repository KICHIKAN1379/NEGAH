package com.kichikan.negah

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val SOUND_PREFERENCES = "negah_sound_preferences"
private const val KEY_BACKGROUND_MUSIC_URI = "background_music_uri"
private const val KEY_BACKGROUND_MUSIC_ENABLED = "background_music_enabled"

@Composable
fun SoundBackgroundThemeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(SOUND_PREFERENCES, Context.MODE_PRIVATE)
    }
    var selectedUri by rememberSaveable {
        mutableStateOf(preferences.getString(KEY_BACKGROUND_MUSIC_URI, null))
    }
    var musicEnabled by rememberSaveable {
        mutableStateOf(preferences.getBoolean(KEY_BACKGROUND_MUSIC_ENABLED, false))
    }
    var selectedName by remember(selectedUri) {
        mutableStateOf(selectedUri?.let { displayName(context, Uri.parse(it)) })
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            selectedUri = uri.toString()
            selectedName = displayName(context, uri)
            preferences.edit().putString(KEY_BACKGROUND_MUSIC_URI, selectedUri).apply()
            BackgroundMusicController.configure(context, selectedUri, musicEnabled)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("صدا، پس‌زمینه و تم", style = MaterialTheme.typography.headlineSmall)
        Text(
            "موسیقی پس‌زمینه فقط هنگام باز بودن برنامه پخش می‌شود و با خروج برنامه متوقف خواهد شد.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("موسیقی انتخاب‌شده: ${selectedName ?: "هنوز فایلی انتخاب نشده است"}")
        OutlinedButton(
            onClick = { audioPicker.launch(arrayOf("audio/mpeg", "audio/mp4", "audio/ogg", "audio/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (selectedUri == null) "انتخاب فایل موسیقی" else "تغییر فایل موسیقی")
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("پخش موسیقی پس‌زمینه", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = musicEnabled,
                enabled = selectedUri != null,
                onCheckedChange = { enabled ->
                    musicEnabled = enabled
                    preferences.edit().putBoolean(KEY_BACKGROUND_MUSIC_ENABLED, enabled).apply()
                    BackgroundMusicController.configure(context, selectedUri, enabled)
                },
            )
        }
        if (selectedUri != null) {
            OutlinedButton(
                onClick = {
                    musicEnabled = false
                    selectedUri = null
                    selectedName = null
                    preferences.edit()
                        .remove(KEY_BACKGROUND_MUSIC_URI)
                        .putBoolean(KEY_BACKGROUND_MUSIC_ENABLED, false)
                        .apply()
                    BackgroundMusicController.configure(context, null, false)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("حذف فایل انتخاب‌شده")
            }
        }
        Text(
            "پس‌زمینهٔ تصویری و گزینه‌های تم در ادامهٔ تکمیل این بخش اضافه می‌شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("بازگشت") }
    }
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

object BackgroundMusicController {
    private var player: MediaPlayer? = null
    private var currentUri: Uri? = null
    private var prepared = false
    private var enabled = false
    private var foreground = false

    @Synchronized
    fun configure(context: Context, uri: String?, shouldPlay: Boolean) {
        val preferences = context.getSharedPreferences(SOUND_PREFERENCES, Context.MODE_PRIVATE)
        preferences.edit()
            .putString(KEY_BACKGROUND_MUSIC_URI, uri)
            .putBoolean(KEY_BACKGROUND_MUSIC_ENABLED, shouldPlay)
            .apply()
        currentUri = uri?.let(Uri::parse)
        enabled = shouldPlay
        synchronize(context.applicationContext)
    }

    @Synchronized
    fun onForeground(context: Context) {
        foreground = true
        val preferences = context.getSharedPreferences(SOUND_PREFERENCES, Context.MODE_PRIVATE)
        currentUri = preferences.getString(KEY_BACKGROUND_MUSIC_URI, null)?.let(Uri::parse)
        enabled = preferences.getBoolean(KEY_BACKGROUND_MUSIC_ENABLED, false)
        synchronize(context.applicationContext)
    }

    @Synchronized
    fun onBackground() {
        foreground = false
        if (player?.isPlaying == true) player?.pause()
    }

    @Synchronized
    fun release() {
        player?.release()
        player = null
        prepared = false
    }

    private fun synchronize(context: Context) {
        val uri = currentUri
        if (!foreground || !enabled || uri == null) {
            if (player?.isPlaying == true) player?.pause()
            return
        }

        if (player != null && currentUri == uri && prepared) {
            if (player?.isPlaying == false) runCatching { player?.start() }
            return
        }

        release()
        val next = MediaPlayer()
        player = next
        next.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        next.isLooping = true
        next.setOnPreparedListener { ready ->
            synchronized(this) {
                if (player === ready) {
                    prepared = true
                    if (foreground && enabled) runCatching { ready.start() }
                }
            }
        }
        next.setOnErrorListener { failed, _, _ ->
            synchronized(this) {
                if (player === failed) release()
            }
            true
        }
        try {
            next.setDataSource(context, uri)
            next.prepareAsync()
        } catch (_: Exception) {
            release()
        }
    }
}

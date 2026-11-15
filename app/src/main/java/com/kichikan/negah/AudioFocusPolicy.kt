package com.kichikan.negah

internal enum class AudioFocusChange {
    GAIN,
    PERMANENT_LOSS,
    TRANSIENT_LOSS,
    TRANSIENT_DUCK,
    OTHER,
}

internal enum class AudioFocusAction {
    NONE,
    PAUSE,
    DUCK,
    RESUME,
    RESTORE_VOLUME,
}

/** Keeps automatic resume limited to interruptions that are explicitly transient. */
internal class AudioFocusPolicy {
    private var resumeOnGain = false
    private var ducked = false

    val isDucked: Boolean get() = ducked

    fun onFocusChange(change: AudioFocusChange): AudioFocusAction = when (change) {
        AudioFocusChange.TRANSIENT_LOSS -> {
            resumeOnGain = true
            ducked = false
            AudioFocusAction.PAUSE
        }
        AudioFocusChange.PERMANENT_LOSS -> {
            reset()
            AudioFocusAction.PAUSE
        }
        AudioFocusChange.TRANSIENT_DUCK -> {
            ducked = true
            AudioFocusAction.DUCK
        }
        AudioFocusChange.GAIN -> when {
            resumeOnGain -> {
                resumeOnGain = false
                ducked = false
                AudioFocusAction.RESUME
            }
            ducked -> {
                ducked = false
                AudioFocusAction.RESTORE_VOLUME
            }
            else -> AudioFocusAction.NONE
        }
        AudioFocusChange.OTHER -> AudioFocusAction.NONE
    }

    fun reset() {
        resumeOnGain = false
        ducked = false
    }
}

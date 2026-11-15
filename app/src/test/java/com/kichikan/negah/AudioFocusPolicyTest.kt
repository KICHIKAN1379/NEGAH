package com.kichikan.negah

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFocusPolicyTest {
    @Test
    fun transientFocusLossPausesAndFocusGainResumes() {
        val policy = AudioFocusPolicy()

        assertEquals(AudioFocusAction.PAUSE, policy.onFocusChange(AudioFocusChange.TRANSIENT_LOSS))
        assertEquals(AudioFocusAction.RESUME, policy.onFocusChange(AudioFocusChange.GAIN))
        assertEquals(AudioFocusAction.NONE, policy.onFocusChange(AudioFocusChange.GAIN))
    }

    @Test
    fun duckingRestoresVolumeWithoutRestartingPlayback() {
        val policy = AudioFocusPolicy()

        assertEquals(AudioFocusAction.DUCK, policy.onFocusChange(AudioFocusChange.TRANSIENT_DUCK))
        assertEquals(AudioFocusAction.RESTORE_VOLUME, policy.onFocusChange(AudioFocusChange.GAIN))
    }

    @Test
    fun permanentFocusLossDoesNotResumeAutomatically() {
        val policy = AudioFocusPolicy()

        assertEquals(AudioFocusAction.PAUSE, policy.onFocusChange(AudioFocusChange.PERMANENT_LOSS))
        assertEquals(AudioFocusAction.NONE, policy.onFocusChange(AudioFocusChange.GAIN))
    }

    @Test
    fun lifecyclePauseClearsPendingTransientResume() {
        val policy = AudioFocusPolicy()
        policy.onFocusChange(AudioFocusChange.TRANSIENT_LOSS)

        policy.reset()

        assertEquals(AudioFocusAction.NONE, policy.onFocusChange(AudioFocusChange.GAIN))
    }
}

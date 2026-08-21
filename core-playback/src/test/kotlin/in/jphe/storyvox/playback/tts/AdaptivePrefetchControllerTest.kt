package `in`.jphe.storyvox.playback.tts

import `in`.jphe.storyvox.playback.PlaybackResourceGovernor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePrefetchControllerTest {
    private val subject = AdaptivePrefetchController()

    @After
    fun resetGovernor() = PlaybackResourceGovernor.reset()

    @Test
    fun `watermarks classify boundaries`() {
        assertEquals(AdaptivePrefetchController.Pressure.CRITICAL, subject.pressure(4_999))
        assertEquals(AdaptivePrefetchController.Pressure.FILLING, subject.pressure(5_000))
        assertEquals(AdaptivePrefetchController.Pressure.TARGET_REACHED, subject.pressure(25_000))
        assertEquals(AdaptivePrefetchController.Pressure.OVER_MAX, subject.pressure(45_000))
    }

    @Test
    fun `generation stops at target and resumes below it`() {
        assertTrue(subject.shouldGenerate(24_999))
        assertFalse(subject.shouldGenerate(25_000))
    }

    @Test
    fun `governor always prioritizes low audio buffer`() {
        PlaybackResourceGovernor.onReadyAudioChanged(4_999, 5_000, 25_000)
        assertEquals(
            PlaybackResourceGovernor.SecondaryWork.SUSPENDED,
            PlaybackResourceGovernor.secondaryWork,
        )
        PlaybackResourceGovernor.onReadyAudioChanged(10_000, 5_000, 25_000)
        assertEquals(
            PlaybackResourceGovernor.SecondaryWork.THROTTLED,
            PlaybackResourceGovernor.secondaryWork,
        )
        PlaybackResourceGovernor.onReadyAudioChanged(25_000, 5_000, 25_000)
        assertEquals(
            PlaybackResourceGovernor.SecondaryWork.ALLOWED,
            PlaybackResourceGovernor.secondaryWork,
        )
    }
}

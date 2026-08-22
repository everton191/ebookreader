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
    fun `parallel reservations count toward target`() {
        assertTrue(subject.canReserve(readyAudioMs = 20_000, reservedAudioMs = 0, estimatedSegmentMs = 4_000))
        assertTrue(subject.canReserve(readyAudioMs = 20_000, reservedAudioMs = 4_000, estimatedSegmentMs = 4_000))
        assertFalse(subject.canReserve(readyAudioMs = 20_000, reservedAudioMs = 8_000, estimatedSegmentMs = 4_000))
    }

    @Test
    fun `reservation cannot project beyond max watermark`() {
        assertFalse(subject.canReserve(readyAudioMs = 10_000, reservedAudioMs = 14_000, estimatedSegmentMs = 22_000))
        assertTrue(subject.canReserve(readyAudioMs = 10_000, reservedAudioMs = 14_000, estimatedSegmentMs = 21_000))
    }

    @Test
    fun `first segment is allowed even when it is longer than max`() {
        assertTrue(subject.canReserve(readyAudioMs = 0, reservedAudioMs = 0, estimatedSegmentMs = 60_000))
    }

    @Test
    fun `slow engine raises adaptive target`() {
        val target = subject.effectiveTargetMs(
            AdaptivePrefetchController.RuntimeSignals(
                rtf = 0.8,
                averageSegmentMs = 9_000,
                engineId = "piper",
            ),
        )
        assertEquals(35_000L, target)
    }

    @Test
    fun `high process ram trims automatic speculation`() {
        val target = subject.effectiveTargetMs(
            AdaptivePrefetchController.RuntimeSignals(
                rtf = 0.2,
                processRamMb = 700,
                queueDepth = 3,
            ),
        )
        assertEquals(20_000L, target)
    }

    @Test
    fun `max remains fixed even for high preset and remote engine`() {
        val signals = AdaptivePrefetchController.RuntimeSignals(
            rtf = 1.2,
            averageSegmentMs = 12_000,
            engineId = "azure",
            qualityPreset = TtsQualityPreset.HIGH,
        )
        assertEquals(35_000L, subject.effectiveTargetMs(signals))
        assertFalse(
            subject.canReserve(
                readyAudioMs = 24_000,
                reservedAudioMs = 10_000,
                estimatedSegmentMs = 12_000,
                signals = signals,
            ),
        )
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

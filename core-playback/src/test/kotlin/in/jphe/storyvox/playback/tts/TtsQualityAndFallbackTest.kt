package `in`.jphe.storyvox.playback.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsQualityAndFallbackTest {
    @Test
    fun `automatic is conservative single-session default`() {
        val preset = TtsQualityPreset.AUTOMATIC
        assertEquals(1, preset.workerCount)
        assertEquals(0, preset.threadsPerWorker)
        assertEquals(25_000, preset.targetPrefetchMs)
        assertTrue(preset.highQualityPitch)
    }

    @Test
    fun `benchmark filters invalid samples and calculates conservative p95`() {
        val summary = TtsBenchmarkCalculator.summarizeRtf(
            listOf(0.20, Double.NaN, -1.0, 0.30, 0.10, 0.40, 0.50),
        )!!
        assertEquals(0.30, summary.first, 0.0001)
        assertEquals(0.50, summary.second, 0.0001)
        assertEquals(
            TtsRealtimeCapability.REALTIME_OK,
            TtsVoiceBenchmark.classifyCapability(summary.second, sampleCount = 5),
        )
    }

    @Test
    fun `slow or under-sampled voice is never advertised as realtime`() {
        assertEquals(
            TtsRealtimeCapability.REALTIME_RISK,
            TtsVoiceBenchmark.classifyCapability(0.2, sampleCount = 2),
        )
        assertEquals(
            TtsRealtimeCapability.OFFLINE_RENDER_ONLY,
            TtsVoiceBenchmark.classifyCapability(1.01, sampleCount = 20),
        )
    }

    @Test
    fun `fallback is bounded and respects available engines`() {
        val policy = TtsFallbackPolicy(maxPrimaryAttempts = 2)
        assertEquals(TtsFallbackPolicy.Action.TRY_PRIMARY, policy.nextAction(0, false, false))
        assertEquals(TtsFallbackPolicy.Action.TRY_PRIMARY, policy.nextAction(1, false, false))
        assertEquals(TtsFallbackPolicy.Action.USE_LOCAL_FALLBACK, policy.nextAction(2, true, true))
        assertEquals(TtsFallbackPolicy.Action.USE_SYSTEM_TTS, policy.nextAction(2, false, true))
        assertEquals(TtsFallbackPolicy.Action.FAIL, policy.nextAction(2, false, false))
        assertFalse(policy.timeoutForAttempt(0) == policy.timeoutForAttempt(1))
    }
}

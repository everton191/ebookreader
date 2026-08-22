package `in`.jphe.storyvox.playback.tts

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class TtsVoiceBenchmarkStoreTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()

    @Before
    fun clear() {
        context.getSharedPreferences("tts_voice_benchmarks_v1", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `persists result by voice model version and preset`() = runTest {
        val store = TtsVoiceBenchmarkStore(context)
        val benchmark = benchmark(modelVersion = "model-v1")

        store.save(benchmark)

        assertEquals(
            benchmark,
            store.get("pt-br-faber", "model-v1", TtsQualityPreset.AUTOMATIC),
        )
        assertNull(store.get("pt-br-faber", "model-v2", TtsQualityPreset.AUTOMATIC))
        assertEquals(benchmark, store.latestForVoice("pt-br-faber"))
    }

    @Test
    fun `recomputes user facing capability after decode`() = runTest {
        val store = TtsVoiceBenchmarkStore(context)
        store.save(benchmark(modelVersion = "slow", p95Rtf = 1.2))

        assertEquals(
            TtsRealtimeCapability.OFFLINE_RENDER_ONLY,
            store.latestForVoice("pt-br-faber")?.capability,
        )
    }

    private fun benchmark(
        modelVersion: String,
        p95Rtf: Double = 0.5,
    ) = TtsVoiceBenchmark(
        engineId = "piper",
        modelId = modelVersion,
        voiceId = "pt-br-faber",
        qualityPreset = TtsQualityPreset.AUTOMATIC,
        modelLoadMs = 1_000,
        warmupMs = 200,
        peakRamMb = 500,
        medianRtf = 0.4,
        p95Rtf = p95Rtf,
        sampleCount = 5,
        measuredAtEpochMs = 123,
    )
}

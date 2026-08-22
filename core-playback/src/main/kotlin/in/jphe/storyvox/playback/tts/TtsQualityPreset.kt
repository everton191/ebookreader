package `in`.jphe.storyvox.playback.tts

import kotlinx.serialization.Serializable

/** Product-level quality choices; engine details stay under the hood. */
@Serializable
enum class TtsQualityPreset(
    val workerCount: Int,
    val threadsPerWorker: Int,
    val targetPrefetchMs: Long,
    val maxPrefetchMs: Long,
    val highQualityPitch: Boolean,
    val cacheQuotaMb: Int,
) {
    AUTOMATIC(
        workerCount = 1,
        threadsPerWorker = 0,
        targetPrefetchMs = 25_000,
        maxPrefetchMs = 45_000,
        highQualityPitch = true,
        cacheQuotaMb = 2_048,
    ),
    ECONOMY(
        workerCount = 1,
        threadsPerWorker = 1,
        targetPrefetchMs = 15_000,
        maxPrefetchMs = 30_000,
        highQualityPitch = false,
        cacheQuotaMb = 500,
    ),
    BALANCED(
        workerCount = 1,
        threadsPerWorker = 0,
        targetPrefetchMs = 25_000,
        maxPrefetchMs = 45_000,
        highQualityPitch = true,
        cacheQuotaMb = 2_048,
    ),
    HIGH(
        workerCount = 1,
        threadsPerWorker = 0,
        targetPrefetchMs = 35_000,
        maxPrefetchMs = 60_000,
        highQualityPitch = true,
        cacheQuotaMb = 5_120,
    ),
}

/** Measured ability of one installed voice/model on this device. */
@Serializable
enum class TtsRealtimeCapability {
    REALTIME_OK,
    REALTIME_RISK,
    OFFLINE_RENDER_ONLY,
}

@Serializable
data class TtsVoiceBenchmark(
    val engineId: String,
    val modelId: String,
    val voiceId: String,
    val qualityPreset: TtsQualityPreset,
    val modelLoadMs: Long,
    val warmupMs: Long,
    val peakRamMb: Long,
    val medianRtf: Double,
    val p95Rtf: Double,
    val sampleCount: Int,
    val measuredAtEpochMs: Long,
) {
    @kotlinx.serialization.Transient
    val capability: TtsRealtimeCapability = classifyCapability(p95Rtf, sampleCount)

    companion object {
        /** Conservative: insufficient data is never advertised as realtime. */
        fun classifyCapability(p95Rtf: Double, sampleCount: Int): TtsRealtimeCapability = when {
            sampleCount < MIN_SAMPLES || !p95Rtf.isFinite() -> TtsRealtimeCapability.REALTIME_RISK
            p95Rtf <= 0.75 -> TtsRealtimeCapability.REALTIME_OK
            p95Rtf <= 1.0 -> TtsRealtimeCapability.REALTIME_RISK
            else -> TtsRealtimeCapability.OFFLINE_RENDER_ONLY
        }

        const val MIN_SAMPLES = 5
    }
}

object TtsBenchmarkCalculator {
    fun summarizeRtf(samples: List<Double>): Pair<Double, Double>? {
        val valid = samples.filter { it.isFinite() && it >= 0.0 }.sorted()
        if (valid.isEmpty()) return null
        val median = percentile(valid, 0.50)
        val p95 = percentile(valid, 0.95)
        return median to p95
    }

    private fun percentile(sorted: List<Double>, fraction: Double): Double {
        val index = kotlin.math.ceil((sorted.size - 1) * fraction).toInt()
        return sorted[index.coerceIn(0, sorted.lastIndex)]
    }
}

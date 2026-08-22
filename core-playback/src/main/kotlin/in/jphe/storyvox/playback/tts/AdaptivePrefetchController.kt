package `in`.jphe.storyvox.playback.tts

/** Time-based watermarks for foreground TTS prefetch. */
class AdaptivePrefetchController(
    val criticalMs: Long = 5_000L,
    val targetMs: Long = 25_000L,
    val maxMs: Long = 45_000L,
) {
    init {
        require(criticalMs >= 0L)
        require(criticalMs < targetMs)
        require(targetMs < maxMs)
    }

    enum class Pressure { CRITICAL, FILLING, TARGET_REACHED, OVER_MAX }

    data class RuntimeSignals(
        val rtf: Double? = null,
        val averageSegmentMs: Long? = null,
        val processRamMb: Long? = null,
        val queueDepth: Int = 0,
        val engineId: String = "unknown",
        val qualityPreset: TtsQualityPreset = TtsQualityPreset.AUTOMATIC,
    )

    fun pressure(readyAudioMs: Long): Pressure = when {
        readyAudioMs < criticalMs -> Pressure.CRITICAL
        readyAudioMs < targetMs -> Pressure.FILLING
        readyAudioMs < maxMs -> Pressure.TARGET_REACHED
        else -> Pressure.OVER_MAX
    }

    /** Generate only while the time buffer is below the target watermark. */
    fun shouldGenerate(readyAudioMs: Long): Boolean = readyAudioMs < targetMs

    /**
     * Reserve estimated audio time before synthesis starts. Counting work in
     * flight prevents N parallel workers from all observing 24.9 s and each
     * producing another long segment past the 45 s ceiling.
     *
     * A single first segment is always allowed for liveness; if that segment
     * itself is longer than [maxMs], it is the only permitted overshoot.
     */
    fun canReserve(
        readyAudioMs: Long,
        reservedAudioMs: Long,
        estimatedSegmentMs: Long,
        signals: RuntimeSignals = RuntimeSignals(),
    ): Boolean {
        val ready = readyAudioMs.coerceAtLeast(0L)
        val reserved = reservedAudioMs.coerceAtLeast(0L)
        val estimate = estimatedSegmentMs.coerceAtLeast(1L)
        if (ready == 0L && reserved == 0L) return true
        val effective = saturatingAdd(ready, reserved)
        val projected = saturatingAdd(effective, estimate)
        return effective < effectiveTargetMs(signals) && projected <= maxMs
    }

    /**
     * Runtime target within the product's 20–35 s operating band. Slow or
     * remote engines gain headroom; large process memory and Economy reduce
     * speculative synthesis. The hard [maxMs] ceiling never moves.
     */
    fun effectiveTargetMs(signals: RuntimeSignals): Long {
        var target = when (signals.qualityPreset) {
            TtsQualityPreset.ECONOMY -> 20_000L
            TtsQualityPreset.BALANCED, TtsQualityPreset.AUTOMATIC -> targetMs
            TtsQualityPreset.HIGH -> 35_000L
        }
        val rtf = signals.rtf
        if (rtf != null && rtf.isFinite()) {
            target += when {
                rtf >= 1.0 -> 10_000L
                rtf >= 0.75 -> 7_500L
                rtf >= 0.45 -> 5_000L
                else -> 0L
            }
        }
        val averageSegmentMs = signals.averageSegmentMs ?: 0L
        if (averageSegmentMs >= 8_000L) target += 5_000L
        if (signals.engineId.contains("azure", ignoreCase = true)) target += 5_000L
        target -= when {
            (signals.processRamMb ?: 0L) >= 900L -> 10_000L
            (signals.processRamMb ?: 0L) >= 650L -> 5_000L
            else -> 0L
        }
        // Empty-queue recovery should never be weakened by RAM trimming.
        if (signals.queueDepth == 0 && rtf != null && rtf >= 0.75) {
            target = maxOf(target, targetMs)
        }
        return target.coerceIn(20_000L, minOf(35_000L, maxMs - 1_000L))
    }

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
}

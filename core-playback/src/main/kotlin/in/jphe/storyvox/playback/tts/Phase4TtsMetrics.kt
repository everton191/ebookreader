package `in`.jphe.storyvox.playback.tts

import android.os.Debug
import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Low-overhead process metrics for the Phase 4 TTS baseline.
 *
 * Counters are process-wide because a chapter transition replaces the PCM
 * source while the TTS model/session intentionally remains alive. Logging is
 * one line per generated segment so adb can reconstruct distributions by
 * engine, voice and segment size without putting storage on the audio path.
 */
object Phase4TtsMetrics {
    private const val TAG = "Phase4Tts"
    private val generated = AtomicLong(0)
    private val generationFailures = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val underruns = AtomicLong(0)
    private val fallbacks = AtomicLong(0)

    fun recordModelLoad(engine: String, voice: String, elapsedMs: Long, reused: Boolean) {
        Log.i(TAG, "MODEL_LOAD_MS=$elapsedMs engine=$engine voice=$voice reused=$reused")
    }

    fun recordWarmup(engine: String, voice: String, elapsedMs: Long, success: Boolean) {
        Log.i(TAG, "TTS_WARMUP_MS=$elapsedMs engine=$engine voice=$voice success=$success")
    }

    fun recordPlayToFirstAudio(elapsedMs: Long, engine: String, voice: String) {
        Log.i(TAG, "PLAY_TO_FIRST_AUDIO_MS=$elapsedMs engine=$engine voice=$voice")
    }

    fun recordSegment(
        engine: String,
        voice: String,
        quality: String,
        textLength: Int,
        generationMs: Long,
        pcmBytes: Int,
        sampleRate: Int,
    ) {
        val durationMs = pcmDurationMs(pcmBytes, sampleRate)
        val rtf = realTimeFactor(generationMs, durationMs)
        generated.incrementAndGet()
        Log.i(
            TAG,
            "SEGMENT_GENERATION_MS=$generationMs SEGMENT_AUDIO_DURATION_MS=$durationMs " +
                "REAL_TIME_FACTOR=${"%.3f".format(java.util.Locale.US, rtf)} " +
                "engine=$engine voice=$voice quality=$quality chars=$textLength RAM_MB=${ramMb()}",
        )
    }

    fun recordGenerationFailure(engine: String, voice: String) {
        val count = generationFailures.incrementAndGet()
        Log.w(TAG, "TTS_FAILURE_COUNT=$count engine=$engine voice=$voice")
    }

    fun recordReadyAudio(milliseconds: Long) {
        Log.i(TAG, "READY_AUDIO_SECONDS=${"%.3f".format(java.util.Locale.US, milliseconds / 1000.0)}")
    }

    fun recordCache(hit: Boolean) {
        if (hit) cacheHits.incrementAndGet() else cacheMisses.incrementAndGet()
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        val rate = if (total == 0L) 0.0 else hits.toDouble() / total
        Log.i(
            TAG,
            "CACHE_HIT_RATE=${"%.3f".format(java.util.Locale.US, rate)} " +
                "CACHE_HITS=$hits CACHE_MISSES=$misses",
        )
    }

    fun recordUnderrun() {
        Log.w(TAG, "QUEUE_UNDERRUN_COUNT=${underruns.incrementAndGet()}")
    }

    fun recordFallback(primary: String, fallback: String) {
        Log.w(
            TAG,
            "TTS_PRIMARY_FAILED=$primary TTS_FALLBACK_USED=$fallback " +
                "FALLBACK_COUNT=${fallbacks.incrementAndGet()}",
        )
    }

    internal fun pcmDurationMs(pcmBytes: Int, sampleRate: Int): Long =
        if (sampleRate <= 0) 0L else pcmBytes.toLong() * 1000L / (sampleRate.toLong() * 2L)

    internal fun realTimeFactor(generationMs: Long, audioDurationMs: Long): Double =
        if (audioDurationMs <= 0L) Double.POSITIVE_INFINITY
        else generationMs.toDouble() / audioDurationMs.toDouble()

    private fun ramMb(): Long = Debug.getPss().toLong() / 1024L
}

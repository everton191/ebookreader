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

    fun pressure(readyAudioMs: Long): Pressure = when {
        readyAudioMs < criticalMs -> Pressure.CRITICAL
        readyAudioMs < targetMs -> Pressure.FILLING
        readyAudioMs < maxMs -> Pressure.TARGET_REACHED
        else -> Pressure.OVER_MAX
    }

    /** Generate only while the time buffer is below the target watermark. */
    fun shouldGenerate(readyAudioMs: Long): Boolean = readyAudioMs < targetMs
}

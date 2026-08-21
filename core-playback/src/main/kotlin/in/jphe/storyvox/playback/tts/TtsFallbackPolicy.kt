package `in`.jphe.storyvox.playback.tts

/** Bounded retry/timeout policy. It never loops and never invents an engine. */
data class TtsFallbackPolicy(
    val primaryTimeoutMs: Long = 15_000,
    val retryTimeoutMs: Long = 5_000,
    val maxPrimaryAttempts: Int = 2,
) {
    init {
        require(primaryTimeoutMs > 0)
        require(retryTimeoutMs > 0)
        require(maxPrimaryAttempts in 1..3)
    }

    enum class Action { TRY_PRIMARY, USE_LOCAL_FALLBACK, USE_SYSTEM_TTS, FAIL }

    fun nextAction(
        primaryAttempts: Int,
        localFallbackAvailable: Boolean,
        systemTtsAvailable: Boolean,
    ): Action = when {
        primaryAttempts < maxPrimaryAttempts -> Action.TRY_PRIMARY
        localFallbackAvailable -> Action.USE_LOCAL_FALLBACK
        systemTtsAvailable -> Action.USE_SYSTEM_TTS
        else -> Action.FAIL
    }

    fun timeoutForAttempt(attempt: Int): Long =
        if (attempt <= 0) primaryTimeoutMs else retryTimeoutMs
}

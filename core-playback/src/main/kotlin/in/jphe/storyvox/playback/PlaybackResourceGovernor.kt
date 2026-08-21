package `in`.jphe.storyvox.playback

import java.util.concurrent.atomic.AtomicReference

/**
 * Shared priority signal for playback-adjacent work. Playback and current TTS
 * always win; downloads and future local AI must observe [secondaryWork].
 */
object PlaybackResourceGovernor {
    enum class SecondaryWork { ALLOWED, THROTTLED, SUSPENDED }

    private val current = AtomicReference(SecondaryWork.ALLOWED)
    val secondaryWork: SecondaryWork get() = current.get()

    fun onReadyAudioChanged(readyAudioMs: Long, criticalMs: Long, targetMs: Long) {
        current.set(
            when {
                readyAudioMs < criticalMs -> SecondaryWork.SUSPENDED
                readyAudioMs < targetMs -> SecondaryWork.THROTTLED
                else -> SecondaryWork.ALLOWED
            },
        )
    }

    fun reset() {
        current.set(SecondaryWork.ALLOWED)
    }
}

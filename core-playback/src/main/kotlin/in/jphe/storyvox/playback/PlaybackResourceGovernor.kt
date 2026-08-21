package `in`.jphe.storyvox.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared priority signal for playback-adjacent work. Playback and current TTS
 * always win; downloads and future local AI must observe [secondaryWork].
 */
object PlaybackResourceGovernor {
    enum class SecondaryWork { ALLOWED, THROTTLED, SUSPENDED }

    private val current = MutableStateFlow(SecondaryWork.ALLOWED)
    val secondaryWork: SecondaryWork get() = current.value
    val secondaryWorkFlow: StateFlow<SecondaryWork> = current.asStateFlow()

    fun onReadyAudioChanged(readyAudioMs: Long, criticalMs: Long, targetMs: Long) {
        current.value =
            when {
                readyAudioMs < criticalMs -> SecondaryWork.SUSPENDED
                readyAudioMs < targetMs -> SecondaryWork.THROTTLED
                else -> SecondaryWork.ALLOWED
            }
    }

    fun reset() {
        current.value = SecondaryWork.ALLOWED
    }
}

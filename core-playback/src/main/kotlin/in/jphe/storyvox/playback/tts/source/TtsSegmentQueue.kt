package `in`.jphe.storyvox.playback.tts.source

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lifecycle of one text segment in the foreground narration pipeline. */
enum class TtsSegmentState {
    WAITING,
    GENERATING,
    READY,
    PLAYING,
    PLAYED,
    FAILED,
}

/** Compact observable view used by diagnostics without copying every segment. */
data class TtsSegmentQueueSnapshot(
    val waiting: Int = 0,
    val generating: Int = 0,
    val ready: Int = 0,
    val playing: Int = 0,
    val played: Int = 0,
    val failed: Int = 0,
    val lastChangedIndex: Int? = null,
    val lastChangedState: TtsSegmentState? = null,
)

/**
 * Thread-safe state ledger shared by serial producer, parallel workers,
 * sequencer and the AudioTrack consumer.
 */
internal class TtsSegmentQueueLedger(
    segmentCount: Int,
    startIndex: Int,
) {
    private val states = Array(segmentCount.coerceAtLeast(0)) { index ->
        if (index < startIndex) TtsSegmentState.PLAYED else TtsSegmentState.WAITING
    }
    private val _snapshot = MutableStateFlow(buildSnapshot())
    val snapshot: StateFlow<TtsSegmentQueueSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun transition(index: Int, state: TtsSegmentState) {
        if (index !in states.indices || states[index] == state) return
        states[index] = state
        _snapshot.value = buildSnapshot(index, state)
    }

    @Synchronized
    fun resetFrom(index: Int) {
        states.indices.forEach { sentenceIndex ->
            states[sentenceIndex] =
                if (sentenceIndex < index) TtsSegmentState.PLAYED else TtsSegmentState.WAITING
        }
        _snapshot.value = buildSnapshot()
    }

    @Synchronized
    fun stateOf(index: Int): TtsSegmentState? = states.getOrNull(index)

    @Synchronized
    fun failUnfinished() {
        states.indices.forEach { index ->
            if (states[index] == TtsSegmentState.GENERATING) {
                states[index] = TtsSegmentState.FAILED
            }
        }
        _snapshot.value = buildSnapshot()
    }

    private fun buildSnapshot(
        changedIndex: Int? = null,
        changedState: TtsSegmentState? = null,
    ): TtsSegmentQueueSnapshot {
        val counts = IntArray(TtsSegmentState.entries.size)
        states.forEach { counts[it.ordinal]++ }
        return TtsSegmentQueueSnapshot(
            waiting = counts[TtsSegmentState.WAITING.ordinal],
            generating = counts[TtsSegmentState.GENERATING.ordinal],
            ready = counts[TtsSegmentState.READY.ordinal],
            playing = counts[TtsSegmentState.PLAYING.ordinal],
            played = counts[TtsSegmentState.PLAYED.ordinal],
            failed = counts[TtsSegmentState.FAILED.ordinal],
            lastChangedIndex = changedIndex,
            lastChangedState = changedState,
        )
    }
}

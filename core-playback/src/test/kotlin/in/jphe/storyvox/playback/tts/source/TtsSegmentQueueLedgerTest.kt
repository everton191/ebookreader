package `in`.jphe.storyvox.playback.tts.source

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsSegmentQueueLedgerTest {
    @Test
    fun `tracks the complete segment lifecycle`() {
        val ledger = TtsSegmentQueueLedger(segmentCount = 3, startIndex = 1)

        assertEquals(TtsSegmentState.PLAYED, ledger.stateOf(0))
        assertEquals(TtsSegmentState.WAITING, ledger.stateOf(1))

        ledger.transition(1, TtsSegmentState.GENERATING)
        ledger.transition(1, TtsSegmentState.READY)
        ledger.transition(1, TtsSegmentState.PLAYING)
        ledger.transition(1, TtsSegmentState.PLAYED)

        assertEquals(TtsSegmentState.PLAYED, ledger.stateOf(1))
        assertEquals(2, ledger.snapshot.value.played)
        assertEquals(1, ledger.snapshot.value.waiting)
    }

    @Test
    fun `seek resets the tail to waiting`() {
        val ledger = TtsSegmentQueueLedger(segmentCount = 4, startIndex = 0)
        ledger.transition(2, TtsSegmentState.FAILED)

        ledger.resetFrom(2)

        assertEquals(TtsSegmentState.PLAYED, ledger.stateOf(1))
        assertEquals(TtsSegmentState.WAITING, ledger.stateOf(2))
        assertEquals(TtsSegmentState.WAITING, ledger.stateOf(3))
    }

    @Test
    fun `shutdown marks in-flight generation failed`() {
        val ledger = TtsSegmentQueueLedger(segmentCount = 2, startIndex = 0)
        ledger.transition(0, TtsSegmentState.GENERATING)

        ledger.failUnfinished()

        assertEquals(TtsSegmentState.FAILED, ledger.stateOf(0))
        assertEquals(1, ledger.snapshot.value.failed)
    }
}

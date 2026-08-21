package `in`.jphe.storyvox.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackResourceGovernorTest {
    @After
    fun reset() = PlaybackResourceGovernor.reset()

    @Test
    fun `ready audio watermarks prioritize playback`() {
        PlaybackResourceGovernor.onReadyAudioChanged(4_999, 5_000, 25_000)
        assertEquals(
            PlaybackResourceGovernor.SecondaryWork.SUSPENDED,
            PlaybackResourceGovernor.secondaryWork,
        )

        PlaybackResourceGovernor.onReadyAudioChanged(5_000, 5_000, 25_000)
        assertEquals(
            PlaybackResourceGovernor.SecondaryWork.THROTTLED,
            PlaybackResourceGovernor.secondaryWork,
        )

        PlaybackResourceGovernor.onReadyAudioChanged(25_000, 5_000, 25_000)
        assertEquals(
            PlaybackResourceGovernor.SecondaryWork.ALLOWED,
            PlaybackResourceGovernor.secondaryWork,
        )
    }

    @Test
    fun `suspended observer resumes only after playback recovers`() = runTest {
        PlaybackResourceGovernor.onReadyAudioChanged(0, 5_000, 25_000)
        val resumed = CompletableDeferred<PlaybackResourceGovernor.SecondaryWork>()
        val observer = launch {
            resumed.complete(
                PlaybackResourceGovernor.secondaryWorkFlow.first {
                    it != PlaybackResourceGovernor.SecondaryWork.SUSPENDED
                },
            )
        }
        advanceUntilIdle()
        assertFalse(resumed.isCompleted)

        PlaybackResourceGovernor.onReadyAudioChanged(10_000, 5_000, 25_000)
        advanceUntilIdle()
        assertTrue(resumed.isCompleted)
        assertEquals(PlaybackResourceGovernor.SecondaryWork.THROTTLED, resumed.await())
        observer.join()
    }
}

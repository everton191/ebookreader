package `in`.jphe.storyvox.playback.cache

import `in`.jphe.storyvox.playback.SentenceRange
import `in`.jphe.storyvox.playback.tts.source.PcmChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TtsRamCacheTest {
    @Test
    fun `reuses exact render and segment identity`() {
        val cache = TtsRamCache(maxBytes = 1_024)
        val key = TtsRamCache.Key("render-a", "7")
        cache.put(key, chunk(7, 100))

        assertNotNull(cache.get(key))
        assertNull(cache.get(TtsRamCache.Key("render-b", "7")))
        assertEquals(1L, cache.stats().hits)
        assertEquals(1L, cache.stats().misses)
    }

    @Test
    fun `evicts least recently used entries within byte limit`() {
        val cache = TtsRamCache(maxBytes = 250)
        val first = TtsRamCache.Key("render", "1")
        val second = TtsRamCache.Key("render", "2")
        val third = TtsRamCache.Key("render", "3")
        cache.put(first, chunk(1, 100))
        cache.put(second, chunk(2, 100))
        cache.get(first) // first is now hotter than second
        cache.put(third, chunk(3, 100))

        assertNull(cache.get(second))
        assertNotNull(cache.get(first))
        assertNotNull(cache.get(third))
        assertEquals(1L, cache.stats().evictions)
        assertEquals(200L, cache.stats().bytes)
    }

    @Test
    fun `oversized segment is not retained`() {
        val cache = TtsRamCache(maxBytes = 64)
        val key = TtsRamCache.Key("render", "large")
        cache.put(key, chunk(1, 65))

        assertNull(cache.get(key))
        assertEquals(0, cache.stats().entries)
        assertEquals(0L, cache.stats().bytes)
    }

    private fun chunk(index: Int, bytes: Int) = PcmChunk(
        sentenceIndex = index,
        range = SentenceRange(index, index * 10, index * 10 + 9),
        pcm = ByteArray(bytes),
        trailingSilenceBytes = 0,
    )
}

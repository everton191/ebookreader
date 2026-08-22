package `in`.jphe.storyvox.playback.cache

import `in`.jphe.storyvox.playback.tts.source.PcmChunk

/**
 * Small process-local LRU for recently synthesized sentence PCM.
 *
 * The render namespace is the full [PcmCacheKey.fileBaseName], so a change in
 * book/chapter text, engine/model version, voice, speed, pitch, style or
 * quality cannot reuse incompatible audio. The on-disk cache remains the
 * durable level; this level exists for immediate seek-back and short pipeline
 * rebuilds while the process is alive.
 */
class TtsRamCache(
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    init {
        require(maxBytes > 0L) { "maxBytes must be positive" }
    }

    data class Key(
        val renderNamespace: String,
        val segmentId: String,
    )

    data class Stats(
        val entries: Int,
        val bytes: Long,
        val hits: Long,
        val misses: Long,
        val evictions: Long,
    ) {
        val hitRate: Double
            get() = if (hits + misses == 0L) 0.0 else hits.toDouble() / (hits + misses)
    }

    private val entries = LinkedHashMap<Key, PcmChunk>(16, 0.75f, true)
    private var bytes = 0L
    private var hits = 0L
    private var misses = 0L
    private var evictions = 0L

    @Synchronized
    fun get(key: Key): PcmChunk? {
        val chunk = entries[key]
        if (chunk == null) misses++ else hits++
        return chunk
    }

    @Synchronized
    fun put(key: Key, chunk: PcmChunk) {
        val chunkBytes = chunk.byteSize()
        entries.remove(key)?.let { bytes -= it.byteSize() }

        // A single pathological sentence must not evict every useful nearby
        // segment and still leave the cache over budget.
        if (chunkBytes > maxBytes) return

        entries[key] = chunk
        bytes += chunkBytes
        val iterator = entries.entries.iterator()
        while (bytes > maxBytes && iterator.hasNext()) {
            val eldest = iterator.next()
            bytes -= eldest.value.byteSize()
            iterator.remove()
            evictions++
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        bytes = 0L
    }

    @Synchronized
    fun stats(): Stats = Stats(
        entries = entries.size,
        bytes = bytes,
        hits = hits,
        misses = misses,
        evictions = evictions,
    )

    private fun PcmChunk.byteSize(): Long = pcm.size.toLong() + trailingSilenceBytes.toLong()

    companion object {
        const val DEFAULT_MAX_BYTES: Long = 16L * 1024L * 1024L
    }
}

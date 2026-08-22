package `in`.jphe.storyvox.playback.tts.source

import kotlin.math.roundToInt

/**
 * Gentle clarity correction for Piper Dii's 16-bit mono PCM. It is a small
 * high-frequency shelf (about +2 dB at the top end), not denoising or a
 * synthetic "studio" effect. Keeping it this mild avoids metallic sibilants.
 */
internal object PcmDiiClarity {
    private const val DII_VOICE_ID = "piper_dii_pt_BR_high"
    private const val DETAIL_MIX = .18f
    private const val PRE_EMPHASIS = .85f

    fun apply(voiceId: String, pcm: ByteArray): ByteArray {
        if (voiceId != DII_VOICE_ID || pcm.size < 4) return pcm
        val output = pcm.copyOf()
        var previous = 0f
        var offset = 0
        while (offset + 1 < output.size) {
            val sample = ((output[offset].toInt() and 0xff) or
                (output[offset + 1].toInt() shl 8)).toShort().toInt()
            val enhanced = sample + DETAIL_MIX * (sample - PRE_EMPHASIS * previous)
            val limited = enhanced.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[offset] = limited.toByte()
            output[offset + 1] = (limited shr 8).toByte()
            previous = sample.toFloat()
            offset += 2
        }
        return output
    }
}

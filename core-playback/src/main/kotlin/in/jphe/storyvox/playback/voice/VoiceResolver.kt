package `in`.jphe.storyvox.playback.voice

/** Metadata-only voice selection. EnginePlayer can consume this without ever waiting for AI. */
data class CharacterCasting(
    val characterId: String,
    val suggestedVoiceId: String? = null,
    val manualVoiceId: String? = null,
    val manualOverride: Boolean = false,
)

data class VoiceResolution(
    val voiceId: String,
    val style: String? = null,
    val speedMultiplier: Float = 1f,
    val pitchMultiplier: Float = 1f,
)

/**
 * Resolves a single temporal segment. It never creates a TTS engine and does
 * not use pitch as a caricature of emotion; engines without native style can
 * apply only these bounded deltas.
 */
object VoiceResolver {
    const val NARRATOR_ID = "__narrator__"

    fun resolve(
        speakerId: String?,
        emotion: String,
        castings: Map<String, CharacterCasting>,
        narratorVoiceId: String?,
        globalVoiceId: String,
    ): VoiceResolution {
        val casting = speakerId?.let(castings::get)
        val voice = when {
            casting?.manualOverride == true && !casting.manualVoiceId.isNullOrBlank() -> casting.manualVoiceId
            !casting?.suggestedVoiceId.isNullOrBlank() -> casting?.suggestedVoiceId
            else -> narratorVoiceId ?: globalVoiceId
        } ?: globalVoiceId
        val adjustment = emotionAdjustment(emotion)
        return VoiceResolution(voice, adjustment.style, adjustment.speed, adjustment.pitch)
    }

    private data class Adjustment(val style: String?, val speed: Float, val pitch: Float)

    private fun emotionAdjustment(emotion: String): Adjustment = when (emotion) {
        "sad" -> Adjustment("sad", .95f, .98f)
        "excited" -> Adjustment("excited", 1.05f, 1.02f)
        "whisper" -> Adjustment("whisper", .96f, .99f)
        "tense", "fear" -> Adjustment(emotion, .97f, 1f)
        "angry" -> Adjustment("angry", 1.02f, 1.01f)
        else -> Adjustment(null, 1f, 1f)
    }
}

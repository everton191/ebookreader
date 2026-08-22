package `in`.jphe.storyvox.playback.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceResolverTest {
    @Test fun `manual casting wins over ai suggestion`() {
        val result = VoiceResolver.resolve(
            speakerId = "maria",
            emotion = "sad",
            castings = mapOf("maria" to CharacterCasting("maria", suggestedVoiceId = "ai", manualVoiceId = "manual", manualOverride = true)),
            narratorVoiceId = "narrator",
            globalVoiceId = "global",
        )
        assertEquals("manual", result.voiceId)
        assertEquals(.95f, result.speedMultiplier)
    }

    @Test fun `unknown speaker uses narrator without exaggerated pitch`() {
        val result = VoiceResolver.resolve("unknown", "excited", emptyMap(), "narrator", "global")
        assertEquals("narrator", result.voiceId)
        assertEquals(1.02f, result.pitchMultiplier)
    }
}

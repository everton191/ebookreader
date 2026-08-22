package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.llm.local.LocalAiProvider
import `in`.jphe.storyvox.llm.local.LocalAiResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

enum class NarrationType { narration, dialogue, thought, other }
enum class NarrationEmotion { neutral, happy, sad, angry, fear, surprise, tender, tense, whisper, excited }

data class NarrationInputSegment(val segmentId: String, val text: String)
data class NarrationMetadata(
    val segmentId: String,
    val type: NarrationType,
    val speaker: String?,
    val emotion: NarrationEmotion,
    val intensity: Float,
    val confidence: Float,
)

/**
 * Converts a bounded chapter window to structured narration metadata. Playback
 * never calls this class synchronously: a caller can persist results later and
 * use neutral narration while they are unavailable.
 */
class NarrationDirector(private val provider: LocalAiProvider) {
    suspend fun analyzeWindow(segments: List<NarrationInputSegment>): List<NarrationMetadata> {
        if (segments.isEmpty()) return emptyList()
        val byId = segments.associateBy { it.segmentId }
        val result = provider.generate(prompt(segments), maxTokens = minOf(1024, segments.size * 80))
        val parsed = (result as? LocalAiResult.Success)?.text?.let(::parseArray).orEmpty()
            .filter { it.segmentId in byId }
            .associateBy { it.segmentId }
        return segments.map { parsed[it.segmentId] ?: heuristic(it) }
    }

    fun textHash(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun prompt(segments: List<NarrationInputSegment>) = buildString {
        append("Você é um diretor de narração em pt-BR. Responda APENAS JSON array. ")
        append("Cada item deve ter segmentId,type,speaker,emotion,intensity,confidence. ")
        append("type: narration|dialogue|thought|other. emotion: neutral|happy|sad|angry|fear|surprise|tender|tense|whisper|excited. ")
        append("Use speaker null quando não houver evidência conservadora. Segmentos:\n")
        segments.forEach { append("[").append(it.segmentId).append("] ").append(it.text).append('\n') }
    }

    private fun parseArray(raw: String): List<NarrationMetadata> = runCatching {
        val payload = raw.substringAfter('[', missingDelimiterValue = "").substringBeforeLast(']', missingDelimiterValue = "")
        if (payload.isEmpty()) return emptyList()
        Json.parseToJsonElement("[$payload]").jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull?.let { runCatching { NarrationType.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
            val emotion = obj["emotion"]?.jsonPrimitive?.contentOrNull?.let { runCatching { NarrationEmotion.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
            val intensity = obj["intensity"]?.jsonPrimitive?.floatOrNull?.takeIf { it in 0f..1f } ?: return@mapNotNull null
            val confidence = obj["confidence"]?.jsonPrimitive?.floatOrNull?.takeIf { it in 0f..1f } ?: return@mapNotNull null
            val id = obj["segmentId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            NarrationMetadata(id, type, obj["speaker"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }, emotion, intensity, confidence)
        }
    }.getOrDefault(emptyList())

    private fun heuristic(segment: NarrationInputSegment): NarrationMetadata {
        val text = segment.text.trim()
        val dialogue = text.startsWith("—") || text.startsWith("\"") || text.startsWith("“")
        val speaker = Regex("(?:disse|gritou|perguntou|respondeu)\\s+([A-ZÀ-Ú][\\p{L}'-]+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)
        val emotion = when {
            Regex("gritou|raiva|furioso|irritad", RegexOption.IGNORE_CASE).containsMatchIn(text) -> NarrationEmotion.angry
            Regex("sussurrou|sussurro", RegexOption.IGNORE_CASE).containsMatchIn(text) -> NarrationEmotion.whisper
            Regex("sorriu|feliz|alegr", RegexOption.IGNORE_CASE).containsMatchIn(text) -> NarrationEmotion.happy
            else -> NarrationEmotion.neutral
        }
        return NarrationMetadata(segment.segmentId, if (dialogue) NarrationType.dialogue else NarrationType.narration, speaker, emotion, if (emotion == NarrationEmotion.neutral) 0f else .35f, if (speaker == null) .35f else .6f)
    }
}

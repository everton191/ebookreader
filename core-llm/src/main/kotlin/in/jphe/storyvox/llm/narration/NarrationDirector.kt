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
        val result = provider.generate(prompt(segments), maxTokens = minOf(768, segments.size * 56))
        var parsed = (result as? LocalAiResult.Success)?.text?.let(::parseArray).orEmpty()
        // LiteRT models occasionally wrap correct JSON in prose or produce an
        // incomplete first answer. One short repair retry is bounded and stays
        // off the playback path; it is preferable to silently classifying a
        // fully downloaded model as "neutral" forever.
        if (parsed.isEmpty() && result is LocalAiResult.Success) {
            android.util.Log.i("NarrationDirector", "invalid JSON; running one repair retry for ${segments.size} segments")
            parsed = (provider.generate(repairPrompt(segments, result.text), maxTokens = minOf(768, segments.size * 56)) as? LocalAiResult.Success)
                ?.text?.let(::parseArray).orEmpty()
        }
        val parsedById = parsed
            .filter { it.segmentId in byId }
            .associateBy { it.segmentId }
        android.util.Log.i("NarrationDirector", "window=${segments.size} parsed=${parsedById.size} fallback=${segments.size - parsedById.size}")
        return segments.map { parsedById[it.segmentId] ?: heuristic(it) }
    }

    fun textHash(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun prompt(segments: List<NarrationInputSegment>) = buildString {
        append("Você é um diretor de narração em pt-BR. Responda APENAS JSON array válido, sem markdown nem explicação. ")
        append("Cada item deve ter segmentId,type,speaker,emotion,intensity,confidence. ")
        append("type: narration|dialogue|thought|other. emotion: neutral|happy|sad|angry|fear|surprise|tender|tense|whisper|excited. ")
        append("Use speaker null quando não houver evidência conservadora. Segmentos:\n")
        segments.forEach { append("[").append(it.segmentId).append("] ").append(it.text).append('\n') }
    }

    private fun repairPrompt(segments: List<NarrationInputSegment>, previous: String) = buildString {
        append("Corrija a resposta abaixo para JSON array válido. Sem markdown e sem explicação. ")
        append("Cada item precisa de segmentId,type,speaker,emotion,intensity,confidence. ")
        append("Use somente os segmentId: ")
        append(segments.joinToString(",") { it.segmentId })
        append(". Resposta anterior: ").append(previous.take(4_000))
    }

    private fun parseArray(raw: String): List<NarrationMetadata> = runCatching {
        val payload = raw.substringAfter('[', missingDelimiterValue = "").substringBeforeLast(']', missingDelimiterValue = "")
        if (payload.isEmpty()) return emptyList()
        Json { ignoreUnknownKeys = true; isLenient = true }
            .parseToJsonElement("[$payload]").jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull?.lowercase()
                ?.let { runCatching { NarrationType.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
            val emotion = obj["emotion"]?.jsonPrimitive?.contentOrNull?.lowercase()
                ?.let { runCatching { NarrationEmotion.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
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

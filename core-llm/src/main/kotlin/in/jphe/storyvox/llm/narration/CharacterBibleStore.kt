package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.data.db.dao.CharacterBibleDao
import `in`.jphe.storyvox.data.db.entity.CharacterBibleEntry
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Conservative identity store: never merges a low-confidence alias automatically. */
class CharacterBibleStore @Inject constructor(private val dao: CharacterBibleDao) {
    /** Stable local key used when a narration analysis first observes a name. */
    fun stableCharacterId(observedName: String): String =
        observedName.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
            .take(80)
            .ifBlank { "character" }

    suspend fun resolveKnownCharacter(fictionId: String, observedName: String): CharacterBibleEntry? {
        val key = normalize(observedName)
        return dao.forFiction(fictionId).firstOrNull { entry ->
            normalize(entry.displayName) == key || aliases(entry).any { normalize(it) == key }
        }
    }

    suspend fun recordSuggestion(
        fictionId: String,
        characterId: String,
        displayName: String,
        aliases: Set<String>,
        confidence: Float,
        suggestedVoiceId: String? = null,
        description: String? = null,
        now: Long = System.currentTimeMillis(),
    ) {
        val existing = resolveKnownCharacter(fictionId, displayName)
        if (existing != null && existing.characterId != characterId && confidence < AUTO_MERGE_CONFIDENCE) return
        val base = existing ?: CharacterBibleEntry(fictionId, characterId, displayName, updatedAt = now)
        // Existing manual choices and aliases are preserved; only high-confidence AI aliases expand them.
        val mergedAliases = if (confidence >= AUTO_MERGE_CONFIDENCE) aliases(base) + aliases else aliases(base)
        dao.upsert(base.copy(
            aliasesJson = encodeAliases(mergedAliases),
            description = base.description ?: description,
            suggestedVoiceId = base.suggestedVoiceId ?: suggestedVoiceId,
            confidence = maxOf(base.confidence, confidence),
            updatedAt = now,
        ))
    }

    suspend fun assignManualVoice(fictionId: String, characterId: String, voiceId: String, now: Long = System.currentTimeMillis()) {
        val existing = dao.forFiction(fictionId).firstOrNull { it.characterId == characterId }
            ?: CharacterBibleEntry(fictionId, characterId, if (characterId == NARRATOR_ID) "Narrador" else characterId, updatedAt = now)
        dao.upsert(existing.copy(manualVoiceId = voiceId, manualOverride = true, updatedAt = now))
    }

    private fun aliases(entry: CharacterBibleEntry): Set<String> = runCatching {
        Json.decodeFromString<List<String>>(entry.aliasesJson).toSet()
    }.getOrDefault(emptySet())

    private fun encodeAliases(aliases: Set<String>): String = Json.encodeToString(aliases.filter { it.isNotBlank() }.sorted())
    private fun normalize(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")

    companion object {
        const val NARRATOR_ID = "__narrator__"
        const val AUTO_MERGE_CONFIDENCE = .85f
    }
}

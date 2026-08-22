package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.data.db.entity.CharacterBibleEntry

/** Applies the invariant manual override > AI suggestion for one book's cast. */
object CastingManager {
    data class Assignment(val characterId: String, val voiceId: String?, val isManual: Boolean)

    fun assignments(entries: List<CharacterBibleEntry>): List<Assignment> = entries.map {
        val manual = it.manualOverride && !it.manualVoiceId.isNullOrBlank()
        Assignment(it.characterId, if (manual) it.manualVoiceId else it.suggestedVoiceId, manual)
    }
}

package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.llm.local.LocalGemmaModelInstaller
import javax.inject.Inject

/**
 * Background entry point for narration analysis. The caller may launch this
 * from a chapter-open/preanalysis worker; it first obeys playback pressure,
 * then stores results. No playback call awaits this coordinator.
 */
class NarrationAnalysisCoordinator @Inject constructor(
    private val gate: AiExecutionGate,
    private val director: NarrationDirector,
    private val store: NarrationPlanStore,
    private val installer: LocalGemmaModelInstaller,
    private val characterBible: CharacterBibleStore,
) {
    suspend fun analyzeAndSave(
        fictionId: String,
        chapterId: String,
        segments: List<NarrationInputSegment>,
        analysisVersion: Int,
        modelVersion: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        // Do not persist heuristic placeholders as if they were Gemma output.
        // If the optional local model is absent, normal neutral playback stays
        // available and a future enablement can analyze this same chapter.
        if (!installer.enabled.value) return false
        if (!store.needsAnalysis(
                fictionId = fictionId,
                chapterId = chapterId,
                source = segments,
                analysisVersion = analysisVersion,
                modelVersion = modelVersion,
                textHash = director::textHash,
            )
        ) return false
        gate.awaitPermit()
        val metadata = director.analyzeWindow(segments)
        store.saveWindow(fictionId, chapterId, segments, metadata, analysisVersion, modelVersion, now, director::textHash)
        // Keep one durable identity per conservatively identified speaker.
        // No voice is invented here: manual casting remains authoritative and
        // the player falls back to the narrator until a compatible choice is
        // present in the CharacterBible.
        metadata.asSequence()
            .filter { it.confidence >= CHARACTER_BIBLE_MIN_CONFIDENCE }
            .mapNotNull { item -> item.speaker?.trim()?.takeIf(String::isNotEmpty)?.let { it to item.confidence } }
            .groupBy({ it.first }, { it.second })
            .forEach { (speaker, confidences) ->
                characterBible.recordSuggestion(
                    fictionId = fictionId,
                    characterId = characterBible.stableCharacterId(speaker),
                    displayName = speaker,
                    aliases = setOf(speaker),
                    confidence = confidences.max(),
                    now = now,
                )
            }
        return true
    }
}

private const val CHARACTER_BIBLE_MIN_CONFIDENCE = .60f

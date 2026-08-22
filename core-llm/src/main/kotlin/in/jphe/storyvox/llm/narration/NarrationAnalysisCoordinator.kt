package `in`.jphe.storyvox.llm.narration

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
) {
    suspend fun analyzeAndSave(
        fictionId: String,
        chapterId: String,
        segments: List<NarrationInputSegment>,
        analysisVersion: Int,
        modelVersion: String,
        now: Long = System.currentTimeMillis(),
    ) {
        gate.awaitPermit()
        val metadata = director.analyzeWindow(segments)
        store.saveWindow(fictionId, chapterId, segments, metadata, analysisVersion, modelVersion, now, director::textHash)
    }
}

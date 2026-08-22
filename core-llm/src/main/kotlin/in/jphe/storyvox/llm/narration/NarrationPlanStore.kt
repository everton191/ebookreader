package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.data.db.dao.NarrationPlanDao
import `in`.jphe.storyvox.data.db.entity.NarrationPlanSegment
import javax.inject.Inject

/** Single persistence boundary for narration analysis; ViewModels never write Room rows directly. */
class NarrationPlanStore @Inject constructor(
    private val narrationPlanDao: NarrationPlanDao,
) {
    /**
     * A completed plan is reusable only when every current playback segment
     * has a row produced by the same model/schema and text content. This
     * avoids re-running the large local model at every Play tap while still
     * invalidating safely after an edited or re-imported chapter.
     */
    suspend fun needsAnalysis(
        fictionId: String,
        chapterId: String,
        source: List<NarrationInputSegment>,
        analysisVersion: Int,
        modelVersion: String,
        textHash: (String) -> String,
    ): Boolean {
        if (source.isEmpty()) return false
        val current = narrationPlanDao.chapterSnapshot(fictionId, chapterId)
        if (current.size != source.size) return true
        val byId = current.associateBy { it.segmentId }
        return source.any { segment ->
            val stored = byId[segment.segmentId]
            stored == null ||
                stored.analysisVersion != analysisVersion ||
                stored.modelVersion != modelVersion ||
                stored.textHash != textHash(segment.text)
        }
    }

    suspend fun saveWindow(
        fictionId: String,
        chapterId: String,
        source: List<NarrationInputSegment>,
        metadata: List<NarrationMetadata>,
        analysisVersion: Int,
        modelVersion: String,
        now: Long,
        textHash: (String) -> String,
    ) {
        val textById = source.associateBy { it.segmentId }
        narrationPlanDao.upsertAll(metadata.mapNotNull { item ->
            val segment = textById[item.segmentId] ?: return@mapNotNull null
            NarrationPlanSegment(
                fictionId = fictionId,
                chapterId = chapterId,
                segmentId = item.segmentId,
                segmentType = item.type.name,
                speaker = item.speaker,
                emotion = item.emotion.name,
                intensity = item.intensity,
                confidence = item.confidence,
                analysisVersion = analysisVersion,
                modelVersion = modelVersion,
                textHash = textHash(segment.text),
                updatedAt = now,
            )
        })
    }

    suspend fun reanalyzeChapter(fictionId: String, chapterId: String) =
        narrationPlanDao.clearChapter(fictionId, chapterId)
}

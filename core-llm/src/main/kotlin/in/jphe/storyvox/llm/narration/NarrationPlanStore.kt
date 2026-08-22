package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.data.db.dao.NarrationPlanDao
import `in`.jphe.storyvox.data.db.entity.NarrationPlanSegment
import javax.inject.Inject

/** Single persistence boundary for narration analysis; ViewModels never write Room rows directly. */
class NarrationPlanStore @Inject constructor(
    private val narrationPlanDao: NarrationPlanDao,
) {
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

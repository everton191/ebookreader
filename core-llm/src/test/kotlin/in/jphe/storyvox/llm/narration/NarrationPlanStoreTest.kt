package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.data.db.dao.NarrationPlanDao
import `in`.jphe.storyvox.data.db.entity.NarrationPlanSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationPlanStoreTest {
    @Test
    fun `does not rerun an unchanged complete chapter plan`() = runTest {
        val dao = FakeNarrationPlanDao()
        val store = NarrationPlanStore(dao)
        val source = listOf(NarrationInputSegment("0", "Ana chegou."))
        store.saveWindow(
            fictionId = "book",
            chapterId = "chapter",
            source = source,
            metadata = listOf(
                NarrationMetadata("0", NarrationType.narration, null, NarrationEmotion.neutral, 0f, 1f),
            ),
            analysisVersion = 1,
            modelVersion = "gemma-v1",
            now = 1L,
            textHash = { it.reversed() },
        )

        assertFalse(store.needsAnalysis("book", "chapter", source, 1, "gemma-v1") { it.reversed() })
    }

    @Test
    fun `reruns when a segment text or model version changes`() = runTest {
        val dao = FakeNarrationPlanDao()
        val store = NarrationPlanStore(dao)
        val original = listOf(NarrationInputSegment("0", "Ana chegou."))
        store.saveWindow(
            "book", "chapter", original,
            listOf(NarrationMetadata("0", NarrationType.narration, null, NarrationEmotion.neutral, 0f, 1f)),
            1, "gemma-v1", 1L, { it.reversed() },
        )

        assertTrue(store.needsAnalysis("book", "chapter", listOf(NarrationInputSegment("0", "Ana saiu.")), 1, "gemma-v1") { it.reversed() })
        assertTrue(store.needsAnalysis("book", "chapter", original, 1, "gemma-v2") { it.reversed() })
    }

    private class FakeNarrationPlanDao : NarrationPlanDao {
        private val rows = mutableListOf<NarrationPlanSegment>()

        override fun observeChapter(fictionId: String, chapterId: String): Flow<List<NarrationPlanSegment>> =
            flowOf(rows.filter { it.fictionId == fictionId && it.chapterId == chapterId })

        override suspend fun chapterSnapshot(fictionId: String, chapterId: String): List<NarrationPlanSegment> =
            rows.filter { it.fictionId == fictionId && it.chapterId == chapterId }

        override suspend fun find(fictionId: String, chapterId: String, segmentId: String): NarrationPlanSegment? =
            rows.firstOrNull { it.fictionId == fictionId && it.chapterId == chapterId && it.segmentId == segmentId }

        override suspend fun upsertAll(segments: List<NarrationPlanSegment>) {
            segments.forEach { segment ->
                rows.removeAll { it.fictionId == segment.fictionId && it.chapterId == segment.chapterId && it.segmentId == segment.segmentId }
                rows += segment
            }
        }

        override suspend fun clearChapter(fictionId: String, chapterId: String) {
            rows.removeAll { it.fictionId == fictionId && it.chapterId == chapterId }
        }
    }
}

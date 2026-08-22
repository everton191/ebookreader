package `in`.jphe.storyvox.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.jphe.storyvox.data.db.entity.NarrationPlanSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface NarrationPlanDao {
    @Query("SELECT * FROM narration_plan_segment WHERE fictionId = :fictionId AND chapterId = :chapterId ORDER BY segmentId")
    fun observeChapter(fictionId: String, chapterId: String): Flow<List<NarrationPlanSegment>>
    @Query("SELECT * FROM narration_plan_segment WHERE fictionId = :fictionId AND chapterId = :chapterId ORDER BY segmentId")
    suspend fun chapterSnapshot(fictionId: String, chapterId: String): List<NarrationPlanSegment>
    @Query("SELECT * FROM narration_plan_segment WHERE fictionId = :fictionId AND chapterId = :chapterId AND segmentId = :segmentId LIMIT 1")
    suspend fun find(fictionId: String, chapterId: String, segmentId: String): NarrationPlanSegment?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(segments: List<NarrationPlanSegment>)
    @Query("DELETE FROM narration_plan_segment WHERE fictionId = :fictionId AND chapterId = :chapterId")
    suspend fun clearChapter(fictionId: String, chapterId: String)
}

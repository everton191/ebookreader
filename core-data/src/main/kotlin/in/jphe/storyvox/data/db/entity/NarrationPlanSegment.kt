package `in`.jphe.storyvox.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Persisted, local metadata produced by the narration director for one segment. */
@Entity(
    tableName = "narration_plan_segment",
    primaryKeys = ["fictionId", "chapterId", "segmentId"],
    foreignKeys = [
        ForeignKey(entity = Fiction::class, parentColumns = ["id"], childColumns = ["fictionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["fictionId"]), Index(value = ["chapterId"])],
)
data class NarrationPlanSegment(
    val fictionId: String,
    val chapterId: String,
    val segmentId: String,
    val segmentType: String,
    val speaker: String? = null,
    val emotion: String,
    val intensity: Float,
    val confidence: Float,
    val analysisVersion: Int,
    val modelVersion: String,
    val textHash: String,
    val updatedAt: Long,
)

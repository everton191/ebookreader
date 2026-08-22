package `in`.jphe.storyvox.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Stable book character identity; an explicit user voice choice always wins. */
@Entity(
    tableName = "character_bible_entry",
    primaryKeys = ["fictionId", "characterId"],
    foreignKeys = [ForeignKey(entity = Fiction::class, parentColumns = ["id"], childColumns = ["fictionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["fictionId", "displayName"], unique = true)],
)
data class CharacterBibleEntry(
    val fictionId: String,
    val characterId: String,
    val displayName: String,
    val aliasesJson: String = "[]",
    val description: String? = null,
    val suggestedVoiceId: String? = null,
    val manualVoiceId: String? = null,
    val manualOverride: Boolean = false,
    val confidence: Float = 0f,
    val updatedAt: Long,
)

package `in`.jphe.storyvox.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.jphe.storyvox.data.db.entity.CharacterBibleEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterBibleDao {
    @Query("SELECT * FROM character_bible_entry WHERE fictionId = :fictionId ORDER BY displayName COLLATE NOCASE")
    fun observeForFiction(fictionId: String): Flow<List<CharacterBibleEntry>>
    @Query("SELECT * FROM character_bible_entry WHERE fictionId = :fictionId")
    suspend fun forFiction(fictionId: String): List<CharacterBibleEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CharacterBibleEntry)
    @Query("DELETE FROM character_bible_entry WHERE fictionId = :fictionId AND characterId = :characterId")
    suspend fun delete(fictionId: String, characterId: String)
}

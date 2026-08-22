package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.data.db.entity.CharacterBibleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CastingManagerTest {
    @Test fun `manual voice overrides suggestion`() {
        val assignment = CastingManager.assignments(listOf(CharacterBibleEntry("book", "maria", "Maria", suggestedVoiceId = "suggested", manualVoiceId = "chosen", manualOverride = true, updatedAt = 0))).single()
        assertEquals("chosen", assignment.voiceId)
        assertTrue(assignment.isManual)
    }
}

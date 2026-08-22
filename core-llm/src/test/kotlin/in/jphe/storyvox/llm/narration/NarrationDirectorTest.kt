package `in`.jphe.storyvox.llm.narration

import `in`.jphe.storyvox.llm.local.LocalAiProvider
import `in`.jphe.storyvox.llm.local.LocalAiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NarrationDirectorTest {
    @Test fun `accepts only valid structured output`() = runTest {
        val director = NarrationDirector(fake("""[{\"segmentId\":\"1\",\"type\":\"dialogue\",\"speaker\":\"Ana\",\"emotion\":\"angry\",\"intensity\":0.7,\"confidence\":0.9}]"""))
        val result = director.analyzeWindow(listOf(NarrationInputSegment("1", "— Eu não quero! — gritou Ana.")))
        assertEquals("Ana", result.single().speaker)
        assertEquals(NarrationEmotion.angry, result.single().emotion)
    }

    @Test fun `falls back conservatively for invalid model output`() = runTest {
        val result = NarrationDirector(fake("texto livre")).analyzeWindow(listOf(NarrationInputSegment("1", "Pedro entrou na sala.")))
        assertEquals(NarrationType.narration, result.single().type)
        assertEquals(NarrationEmotion.neutral, result.single().emotion)
    }

    @Test fun `accepts a lenient JSON array wrapped by the local model`() = runTest {
        val result = NarrationDirector(fake("Resultado: [{segmentId: \"1\", type: \"DIALOGUE\", speaker: \"Ana\", emotion: \"ANGRY\", intensity: 0.7, confidence: 0.9}]"))
            .analyzeWindow(listOf(NarrationInputSegment("1", "— Eu não quero! — gritou Ana.")))
        assertEquals("Ana", result.single().speaker)
        assertEquals(NarrationEmotion.angry, result.single().emotion)
    }

    private fun fake(response: String) = object : LocalAiProvider {
        override suspend fun generate(prompt: String, maxTokens: Int) = LocalAiResult.Success(response)
        override suspend fun unload() = Unit
    }
}

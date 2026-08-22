package `in`.jphe.storyvox.llm.narration

/** Playback-owned admission control for expensive local AI work. */
interface AiExecutionGate {
    suspend fun awaitPermit()
}

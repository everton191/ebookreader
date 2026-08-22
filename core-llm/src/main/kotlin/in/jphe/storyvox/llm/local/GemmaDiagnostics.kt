package `in`.jphe.storyvox.llm.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Runtime-only measurements; zero/null means no real operation has occurred yet. */
data class GemmaDiagnosticsSnapshot(
    val runtimeState: String = "UNLOADED",
    val loadMs: Long? = null,
    val analysisMs: Long? = null,
    val estimatedTokensPerSecond: Float? = null,
    val processPssKb: Long? = null,
    val analyses: Long = 0,
    val failures: Long = 0,
)

@Singleton
class GemmaDiagnostics @Inject constructor() {
    private val _state = MutableStateFlow(GemmaDiagnosticsSnapshot())
    val state: StateFlow<GemmaDiagnosticsSnapshot> = _state.asStateFlow()

    fun loading() { _state.value = _state.value.copy(runtimeState = "LOADING") }
    fun ready(loadMs: Long) { _state.value = _state.value.copy(runtimeState = "READY", loadMs = loadMs, processPssKb = pssKb()) }
    fun running() { _state.value = _state.value.copy(runtimeState = "RUNNING") }
    fun completed(prompt: String, response: String, elapsedMs: Long) {
        val tokens = ((prompt.length + response.length) / 4).coerceAtLeast(1)
        val rate = tokens * 1_000f / elapsedMs.coerceAtLeast(1)
        _state.value = _state.value.copy(runtimeState = "READY", analysisMs = elapsedMs, estimatedTokensPerSecond = rate, processPssKb = pssKb(), analyses = _state.value.analyses + 1)
    }
    fun failed() { _state.value = _state.value.copy(runtimeState = "READY", failures = _state.value.failures + 1, processPssKb = pssKb()) }
    fun unloaded() { _state.value = _state.value.copy(runtimeState = "UNLOADED", processPssKb = pssKb()) }

    private fun pssKb(): Long? = runCatching {
        android.os.Debug.MemoryInfo().also(android.os.Debug::getMemoryInfo).getTotalPss().toLong()
    }.getOrNull()
}

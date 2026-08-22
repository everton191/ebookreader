package `in`.jphe.storyvox.llm.local

import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Contrato separado dos provedores de rede legados: sem fallback para nuvem. */
interface LocalAiProvider {
    suspend fun generate(prompt: String, maxTokens: Int = 768): LocalAiResult
    suspend fun unload()
}

sealed interface LocalAiResult {
    data class Success(val text: String) : LocalAiResult
    data class Unavailable(val reason: String) : LocalAiResult
    data class Failure(val reason: String, val cause: Throwable? = null) : LocalAiResult
}

/**
 * Provedor fail-closed. Se o arquivo nao foi instalado e validado, nenhuma
 * chamada e enviada a providers existentes; a analise recebe Unavailable e o
 * leitor continua pelo caminho heuristico/local.
 */
class LiteRtGemmaAiProvider(
    private val installer: LocalGemmaModelInstaller,
) : LocalAiProvider {
    private val mutex = Mutex()
    private var engine: Engine? = null

    override suspend fun generate(prompt: String, maxTokens: Int): LocalAiResult = mutex.withLock {
        val model = installer.installedFileOrNull()
            ?: return@withLock LocalAiResult.Unavailable("Modelo local nao instalado ou sem integridade valida")
        try {
            val activeEngine = engine ?: createEngine(model, maxTokens).also { engine = it }
            val reply = withContext(Dispatchers.Default) {
                activeEngine.createConversation().use { conversation ->
                    conversation.sendMessage(prompt).contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString(separator = "") { it.text }
                }
            }
            LocalAiResult.Success(reply)
        } catch (t: Throwable) {
            LocalAiResult.Failure("Falha na inferencia local: ${t.message ?: t.javaClass.simpleName}", t)
        }
    }

    override suspend fun unload() = mutex.withLock {
        engine?.close()
        engine = null
    }

    private fun createEngine(model: File, maxTokens: Int): Engine =
        Engine(EngineConfig(modelPath = model.absolutePath, maxNumTokens = maxTokens)).also(Engine::initialize)
}

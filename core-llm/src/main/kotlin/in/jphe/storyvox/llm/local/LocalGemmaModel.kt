package `in`.jphe.storyvox.llm.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** Informacoes imutaveis do unico modelo de analise local aceito nesta fase. */
data class LocalModelManifest(
    val id: String,
    val version: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String,
    val minRamBytes: Long,
    val runtime: String,
    val quantization: String,
)

object Gemma4E2bManifest {
    val value = LocalModelManifest(
        id = "gemma-4-E2B-it",
        version = "litert-community-main",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
        sizeBytes = 2_588_147_712L,
        sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
        minRamBytes = 8L * 1024L * 1024L * 1024L,
        runtime = "LiteRT-LM 0.16.1",
        quantization = "Q4",
    )
}

sealed interface LocalModelState {
    data object NotInstalled : LocalModelState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : LocalModelState
    data object Verifying : LocalModelState
    data class Installed(val file: File) : LocalModelState
    data class Failed(val reason: String) : LocalModelState
}

/**
 * Instalacao explicitamente iniciada pela tela de IA. Nunca e chamada pelo
 * playback: mantem .part para retomar, confere SHA-256 e so entao promove o
 * arquivo para o nome definitivo.
 */
class LocalGemmaModelInstaller(
    context: Context,
    private val client: OkHttpClient,
    val manifest: LocalModelManifest = Gemma4E2bManifest.value,
) {
    private val modelsDir = File(context.filesDir, "local-models").apply { mkdirs() }
    private val target = File(modelsDir, "${manifest.id}.litertlm")
    private val partial = File(modelsDir, "${manifest.id}.litertlm.part")
    private val _state = MutableStateFlow<LocalModelState>(initialState())
    val state: StateFlow<LocalModelState> = _state.asStateFlow()

    fun installedFileOrNull(): File? = target.takeIf { it.isFile && it.length() == manifest.sizeBytes }

    suspend fun install() = withContext(Dispatchers.IO) {
        if (isVerified(target)) {
            _state.value = LocalModelState.Installed(target)
            return@withContext
        }
        val existing = partial.takeIf(File::exists)?.length() ?: 0L
        val request = Request.Builder().url(manifest.downloadUrl).apply {
            if (existing > 0L) header("Range", "bytes=$existing-")
        }.build()
        try {
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download falhou (HTTP ${response.code})" }
                val append = existing > 0L && response.code == 206
                if (!append && partial.exists()) partial.delete()
                val start = if (append) existing else 0L
                val expected = response.body?.contentLength()?.takeIf { it >= 0L }?.plus(start) ?: manifest.sizeBytes
                response.body!!.byteStream().use { input ->
                    FileOutputStream(partial, append).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = start
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            _state.value = LocalModelState.Downloading(copied, expected)
                        }
                    }
                }
            }
            _state.value = LocalModelState.Verifying
            check(isVerified(partial)) { "Integridade do modelo invalida; o download parcial foi preservado para diagnostico." }
            check(partial.renameTo(target)) { "Nao foi possivel instalar o modelo de forma atomica." }
            _state.value = LocalModelState.Installed(target)
        } catch (t: Throwable) {
            _state.value = LocalModelState.Failed(t.message ?: "Falha desconhecida ao instalar o modelo")
            throw t
        }
    }

    private fun initialState(): LocalModelState =
        if (isVerified(target)) LocalModelState.Installed(target) else LocalModelState.NotInstalled

    private fun isVerified(file: File): Boolean {
        if (!file.isFile || file.length() != manifest.sizeBytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == manifest.sha256
    }
}

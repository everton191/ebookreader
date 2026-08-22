package `in`.jphe.storyvox.data

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.jphe.storyvox.feature.api.UiVoice
import `in`.jphe.storyvox.feature.api.VoiceProviderUi
import `in`.jphe.storyvox.playback.voice.EngineType
import `in`.jphe.storyvox.playback.voice.VoiceManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Compatibility surface used by SettingsViewModel. VoiceManager is the
 * canonical source for install/select/download; this adapter exposes its real
 * installed local-neural voices to Azure's offline-fallback picker.
 */
@Singleton
class VoiceProviderUiImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceManager: VoiceManager,
) : VoiceProviderUi {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engineResolver = TtsEngineResolver(context)

    override val installedVoices: Flow<List<UiVoice>> = voiceManager.installedVoices
        .map { installed ->
            installed.asSequence()
                // This legacy surface is consumed by Settings' Azure fallback
                // picker. Only genuinely local neural voices are valid there;
                // choosing Azure would recurse and System TTS ids do not match
                // VoiceManager's catalog fallback contract.
                .filter { it.engineType !is EngineType.Azure }
                .filter { it.engineType !is EngineType.SystemTts }
                .map { voice ->
                    UiVoice(
                        id = voice.id,
                        label = voice.displayName,
                        engine = voice.engineType.toString(),
                        locale = voice.language.replace('_', '-'),
                    )
                }
                .sortedWith(compareBy({ it.locale }, { it.label }))
                .toList()
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun previewVoice(voice: UiVoice) {
        // Kept for the legacy VoiceProviderUi contract. The focused Azure
        // settings screen does not call preview; local-neural previews are
        // handled by the Voice Library/EnginePlayer path.
        scope.launch {
            val tts = bootTts() ?: return@launch
            try {
                runCatching {
                    tts.voices?.firstOrNull { it.name == voice.id }?.let { tts.voice = it }
                    tts.speak(PREVIEW_TEXT, TextToSpeech.QUEUE_FLUSH, null, "preview-${voice.id}")
                }
                // Let the utterance play, then release.
                kotlinx.coroutines.delay(4_000L)
            } finally {
                runCatching { tts.shutdown() }
            }
        }
    }

    /**
     * Boot a one-shot Android [TextToSpeech] bound to an explicit public
     * engine. #1384 — a null target asks the framework to bind the device
     * default, which on Samsung is a private engine whose refused bind
     * spins a connect/disconnect loop that never fires onInit. The await
     * is timeout-bounded and cancellation tears the instance down so a
     * stuck init can't leak the instance into that loop.
     */
    private suspend fun bootTts(): TextToSpeech? {
        // #1392 — any TextToSpeech on Samsung triggers the infinite
        // reconnect loop via the framework's internal private-engine
        // probe. Skip entirely; system TTS voices are hidden on Samsung
        // anyway (SystemTtsVoiceRoster returns empty), so nothing to
        // boot for.
        if (engineResolver.isSamsungDevice) return null
        return withTimeoutOrNull(INIT_TIMEOUT_MS) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                var tts: TextToSpeech? = null
                val onInit = TextToSpeech.OnInitListener { status ->
                    if (cont.isCompleted) return@OnInitListener
                    if (status == TextToSpeech.SUCCESS) {
                        cont.resume(tts) { runCatching { tts?.shutdown() } }
                    } else {
                        runCatching { tts?.shutdown() }
                        cont.resume(null) {}
                    }
                }
                val engine = engineResolver.preferredPublicEngine()
                tts = if (engine.isNullOrBlank()) {
                    TextToSpeech(context, onInit)
                } else {
                    TextToSpeech(context, onInit, engine)
                }
                cont.invokeOnCancellation { runCatching { tts?.shutdown() } }
            }
        }
    }

    private companion object {
        const val PREVIEW_TEXT = "A luz da varanda acendeu. Bem-vindo de volta à sua biblioteca."

        /** #1384 — ceiling on the onInit await so a stuck engine init
         *  can't suspend (and leak) the instance forever. */
        const val INIT_TIMEOUT_MS: Long = 8_000
    }
}

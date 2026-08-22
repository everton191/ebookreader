package `in`.jphe.storyvox.playback.cache

import `in`.jphe.storyvox.playback.voice.EngineType
import `in`.jphe.storyvox.playback.voice.UiVoiceInfo
import `in`.jphe.storyvox.playback.voice.VoiceCatalog
import `in`.jphe.storyvox.playback.voice.VoiceManager

/** Stable cache identity for the exact installed model payload. */
internal fun cacheModelVersionFor(voiceManager: VoiceManager, active: UiVoiceInfo): String {
    val piperPins = VoiceCatalog.byId(active.id)?.piper?.let { paths ->
        listOfNotNull(paths.onnxSha256, paths.tokensSha256).joinToString(":")
    }.orEmpty()
    if (piperPins.isNotBlank()) return piperPins

    val modelDirectory = when (active.engineType) {
        EngineType.Piper -> voiceManager.voiceDirFor(active.id)
        is EngineType.Kokoro -> voiceManager.kokoroSharedDir()
        is EngineType.Kitten -> voiceManager.kittenSharedDir()
        is EngineType.Supertonic -> voiceManager.supertonicSharedDir()
        is EngineType.Azure -> return active.engineType.toString()
        is EngineType.SystemTts -> return active.engineType.toString()
    }
    return modelDirectory.listFiles()
        .orEmpty()
        .filter { it.isFile && !it.name.endsWith(".part") }
        .sortedBy { it.name }
        .joinToString("|") { "${it.name}:${it.length()}:${it.lastModified()}" }
        .ifBlank { "${active.id}:unknown" }
}

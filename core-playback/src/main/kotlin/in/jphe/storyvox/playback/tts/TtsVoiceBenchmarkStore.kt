package `in`.jphe.storyvox.playback.tts

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persistent, model-versioned benchmark results for installed voices. */
@Singleton
class TtsVoiceBenchmarkStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(
        voiceId: String,
        modelVersion: String,
        qualityPreset: TtsQualityPreset,
    ): TtsVoiceBenchmark? = withContext(Dispatchers.IO) {
        val encoded = preferences.getString(key(voiceId, modelVersion, qualityPreset), null)
            ?: return@withContext null
        runCatching { json.decodeFromString<TtsVoiceBenchmark>(encoded) }.getOrNull()
    }

    suspend fun save(benchmark: TtsVoiceBenchmark) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString(
                key(benchmark.voiceId, benchmark.modelId, benchmark.qualityPreset),
                json.encodeToString(benchmark),
            )
            .putString(latestKey(benchmark.voiceId), json.encodeToString(benchmark))
            .commit()
    }

    /** Fast read used while projecting voice-library rows. */
    fun latestForVoice(voiceId: String): TtsVoiceBenchmark? {
        val encoded = preferences.getString(latestKey(voiceId), null) ?: return null
        return runCatching { json.decodeFromString<TtsVoiceBenchmark>(encoded) }.getOrNull()
    }

    private fun key(
        voiceId: String,
        modelVersion: String,
        qualityPreset: TtsQualityPreset,
    ): String {
        val identity = "$voiceId|$modelVersion|${qualityPreset.name}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "benchmark_$digest"
    }

    private fun latestKey(voiceId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(voiceId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "latest_$digest"
    }

    private companion object {
        const val PREFERENCES_NAME = "tts_voice_benchmarks_v1"
    }
}

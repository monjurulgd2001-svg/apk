package com.zedge.automation.data

import com.zedge.automation.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Stable Audio music generation — identical endpoint, payload and
 * poll-until-200 flow as genSAudio()/pollAudio() in main.js.
 *
 * Throws [StableAudioAuthException] on 401/429 so the caller can
 * trigger auto-recovery (new account creation via WebView).
 */
class StableAudioClient(private val settings: SettingsStore) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /** Check if token is set and looks valid (non-empty, starts with "eyJ" for JWT). */
    fun isTokenValid(): Boolean {
        val token = settings.stableAudioToken
        return token.isNotBlank() && token.startsWith("eyJ")
    }

    /** Quick validation call — hits a lightweight endpoint to check if token works. */
    suspend fun validateToken(): Boolean = withContext(Dispatchers.IO) {
        val token = settings.stableAudioToken
        if (token.isBlank()) return@withContext false
        try {
            // Use the generate endpoint with minimal payload to test auth
            val body = JSONObject().put("data", JSONObject()
                .put("type", "generations")
                .put("attributes", JSONObject()
                    .put("prompts", JSONArray().put(JSONObject().put("text", "test").put("weight", 1)))
                    .put("length_seconds", 5)
                    .put("seed", 0)))
            val req = Request.Builder()
                .url(AppConfig.STABLE_AUDIO_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute().use { r ->
                r.code != 401 && r.code != 403
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Starts a generation; returns the result polling URL. */
    suspend fun generate(prompt: String, lengthSeconds: Int): String = withContext(Dispatchers.IO) {
        val token = settings.stableAudioToken
        if (token.isBlank()) throw StableAuthRequiredException("Stable Audio token not set. Please login first.")
        val seed = Random.nextInt(-32768, 32768)
        val body = JSONObject().put("data", JSONObject()
            .put("type", "generations")
            .put("attributes", JSONObject()
                .put("prompts", JSONArray().put(JSONObject().put("text", prompt).put("weight", 1)))
                .put("length_seconds", lengthSeconds)
                .put("seed", seed)))
        val req = Request.Builder()
            .url(AppConfig.STABLE_AUDIO_URL)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { r ->
            val text = r.body?.string() ?: ""
            when (r.code) {
                200 -> JSONObject(text).getJSONArray("data").getJSONObject(0)
                    .getJSONObject("links").getString("result")
                401, 403 -> throw StableAuthRequiredException(
                    "Token expired or invalid (HTTP ${r.code}). Please create a new account."
                )
                429 -> throw StableAuthRequiredException(
                    "API rate limit reached (HTTP 429). Auto-recovery: creating new account..."
                )
                else -> throw Exception("Stable Audio (${r.code}): ${text.take(200)}")
            }
        }
    }

    /** Poll every 3s (max 60 tries = 3 min) until HTTP 200, then return audio bytes. */
    suspend fun poll(url: String, onTick: ((Int, Int) -> Unit)? = null): ByteArray = withContext(Dispatchers.IO) {
        val token = settings.stableAudioToken
        repeat(60) { i ->
            onTick?.invoke(i, 60)
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "*/*")
                .build()
            http.newCall(req).execute().use { r ->
                when (r.code) {
                    200 -> return@withContext r.body!!.bytes()
                    202 -> { /* still composing */ }
                    401, 403 -> throw StableAuthRequiredException(
                        "Token expired during polling (HTTP ${r.code})."
                    )
                    429 -> throw StableAuthRequiredException(
                        "API rate limit reached during polling (HTTP 429)."
                    )
                    else -> throw Exception("Download fail: HTTP ${r.code}")
                }
            }
            delay(3000)
        }
        throw Exception("Timed out (3 min).")
    }
}

/**
 * Thrown when Stable Audio API returns 401/403/429.
 * The caller should trigger auto-recovery (create new account via WebView).
 */
class StableAuthRequiredException(message: String) : Exception(message)

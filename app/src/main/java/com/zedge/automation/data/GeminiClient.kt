package com.zedge.automation.data

import com.zedge.automation.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gemini auto-metadata — identical API contract, key rotation, retry
 * behaviour and prompts as the web dashboard (main.js).
 * Falls back to Mistral when Gemini fails.
 */
class GeminiClient(private val settings: SettingsStore, private val mistral: MistralClient? = null) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val keyIndex = AtomicInteger(0)

    private fun nextKey(): String {
        val keys = settings.geminiApiKeys
        if (keys.isEmpty()) throw Exception("No Gemini keys.")
        val i = keyIndex.getAndIncrement()
        return keys[Math.floorMod(i, keys.size)]
    }

    private fun endpoint(model: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    private suspend fun callGemini(bodyParts: JSONArray, retries: Int = 3): String =
        withContext(Dispatchers.IO) {
            var lastErr: Exception? = null
            for (attempt in 1..retries) {
                try {
                    val key = nextKey()
                    val model = settings.geminiModel.ifBlank { AppConfig.DEFAULT_GEMINI_MODEL }
                    val body = JSONObject()
                        .put("contents", JSONArray().put(JSONObject().put("parts", bodyParts)))
                        .put("generationConfig", JSONObject().put("temperature", 1).put("maxOutputTokens", 8192))
                    val req = Request.Builder()
                        .url(endpoint(model))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", key)
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    http.newCall(req).execute().use { r ->
                        val text = r.body?.string() ?: ""
                        if (!r.isSuccessful) throw Exception("Gemini ${r.code}: ${text.take(200)}")
                        val json = JSONObject(text)
                        val c = json.optJSONArray("candidates")?.optJSONObject(0)
                            ?: throw Exception("Gemini blocked/empty: no content")
                        val parts = c.optJSONObject("content")?.optJSONArray("parts")
                            ?: throw Exception("Gemini blocked/empty: ${c.optString("finishReason", "no content")}")
                        val out = buildString {
                            for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text") ?: "")
                        }.trim()
                        if (out.isEmpty()) throw Exception("Gemini returned empty text")
                        return@withContext out
                    }
                } catch (e: Exception) {
                    lastErr = e
                    if (attempt < retries) delay(2000L * attempt)
                }
            }
            throw lastErr ?: Exception("Gemini failed")
        }

    suspend fun gemini(text: String): String {
        return try {
            callGemini(JSONArray().put(JSONObject().put("text", text)))
        } catch (e: Exception) {
            if (mistral != null && settings.hasMistralKeys()) {
                try { mistral.mistral(text) } catch (e2: Exception) { throw e }
            } else throw e
        }
    }

    suspend fun geminiWithImage(base64Jpeg: String, prompt: String): String {
        return try {
            callGemini(
                JSONArray()
                    .put(JSONObject().put("inlineData",
                        JSONObject().put("mimeType", "image/jpeg").put("data", base64Jpeg)))
                    .put(JSONObject().put("text", prompt))
            )
        } catch (e: Exception) {
            if (mistral != null && settings.hasMistralKeys()) {
                try { mistral.mistralWithImage(base64Jpeg, prompt) } catch (e2: Exception) { throw e }
            } else throw e
        }
    }

    private fun cleanTitle(raw: String, existingTitles: List<String>): String {
        var t = raw.trim('"', '\'').replace(Regex("[:\\-]"), "")
            .replace(Regex("\\s+"), " ").trim().take(50)
        if (existingTitles.contains(t.lowercase())) {
            val suffix = (1..3).map { ('A'..'Z').random() }.joinToString("")
            t = t.take(44) + " " + suffix
        }
        return t
    }

    private fun cleanTags(kw: String): String =
        kw.split(",")
            .map { it.replace(Regex("[^a-zA-Z0-9]"), "") }
            .filter { it.isNotEmpty() && it.length <= 24 }
            .take(10)
            .joinToString(", ")

    /** Wallpaper: 4 parallel Gemini vision calls — same prompts as analyzeImg() */
    suspend fun analyzeImage(base64Jpeg: String, existingTitles: List<String> = emptyList()): MetaData = coroutineScope {
        val avoid = if (existingTitles.isNotEmpty())
            "\nAVOID these existing titles (do NOT generate similar or same): ${existingTitles.take(30).joinToString(", ")}" else ""
        val title = async { runCatching { geminiWithImage(base64Jpeg, "Creative 2-3 word title. Only title.$avoid") }.getOrNull() }
        val kw = async { runCatching { geminiWithImage(base64Jpeg, "15 SINGLE-WORD keywords. Comma-separated.") }.getOrNull() }
        val cat = async { runCatching { geminiWithImage(base64Jpeg, "Category: ${AppConfig.AI_IMG_CATS.joinToString(", ")}. Return ONLY name.") }.getOrNull() }
        val desc = async { runCatching { geminiWithImage(base64Jpeg, "Description MAX 90 chars.") }.getOrNull() }
        val results = awaitAll(title, kw, cat, desc)
        if (results[0] == null && results[1] == null) throw Exception("All AI calls failed")
        val rc = (results[2] ?: "").replace(Regex("[^A-Z_]"), "").trim()
        MetaData(
            title = cleanTitle(results[0] ?: "Wallpaper", existingTitles),
            tags = cleanTags(results[1] ?: ""),
            category = if (AppConfig.AI_IMG_CATS.contains(rc)) rc else "OTHER",
            description = (results[3] ?: "").trim('"', '\'').replace(Regex("[:\\-]"), "").take(90)
        )
    }

    /** Ringtone: 4 parallel Gemini text calls — same prompts as genMeta() */
    suspend fun genMeta(prompt: String, existingTitles: List<String> = emptyList()): MetaData = coroutineScope {
        val avoid = if (existingTitles.isNotEmpty())
            "\nAVOID these existing titles (do NOT generate similar or same): ${existingTitles.take(30).joinToString(", ")}" else ""
        val title = async { gemini("Prompt: \"$prompt\"\nCatchy 2-3 word title. NO BPM. Return ONLY title.$avoid") }
        val kw = async { gemini("Prompt: \"$prompt\"\n15 SINGLE-WORD keywords. Comma-separated.") }
        val cat = async { gemini("Prompt: \"$prompt\"\nChoose: ${AppConfig.AI_RING_CATS.joinToString(", ")}. Return ONLY name.") }
        val desc = async { gemini("Prompt: \"$prompt\"\nMAX 5 WORDS description. No punctuation.") }
        val rc = cat.await().replace(Regex("[^A-Z_]"), "").trim()
        MetaData(
            title = cleanTitle(title.await(), existingTitles),
            tags = cleanTags(kw.await()),
            category = if (AppConfig.AI_RING_CATS.contains(rc)) rc else "OTHER",
            description = desc.await().trim('"', '\'').replace(Regex("[:\\-]"), "").trim()
                .split(Regex("\\s+")).take(5).joinToString(" ")
        )
    }
}

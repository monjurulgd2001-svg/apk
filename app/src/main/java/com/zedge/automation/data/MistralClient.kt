package com.zedge.automation.data

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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mistral AI client — fallback when Gemini fails.
 * Same key rotation + retry pattern as GeminiClient.
 * v3.1: supports strict JSON mode (response_format json_object) for metadata.
 */
class MistralClient(private val settings: SettingsStore) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val keyIndex = AtomicInteger(0)

    private fun nextKey(): String {
        val keys = settings.mistralApiKeys
        if (keys.isEmpty()) throw Exception("No Mistral keys.")
        val i = keyIndex.getAndIncrement()
        return keys[Math.floorMod(i, keys.size)]
    }

    private val endpoint = "https://api.mistral.ai/v1/chat/completions"

    private suspend fun callMistral(
        messages: JSONArray,
        temperature: Double = 0.9,
        maxTokens: Int = 512,
        modelOverride: String? = null,
        jsonMode: Boolean = false,
        retries: Int = 3
    ): String = withContext(Dispatchers.IO) {
        var lastErr: Exception? = null
        for (attempt in 1..retries) {
            try {
                val key = nextKey()
                val model = modelOverride ?: settings.mistralModel.ifBlank { "mistral-small-latest" }
                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", temperature)
                    .put("max_tokens", maxTokens)
                if (jsonMode) body.put("response_format", JSONObject().put("type", "json_object"))
                val req = Request.Builder()
                    .url(endpoint)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer $key")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(req).execute().use { r ->
                    val text = r.body?.string() ?: ""
                    if (!r.isSuccessful) throw Exception("Mistral ${r.code}: ${text.take(200)}")
                    val json = JSONObject(text)
                    val out = json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content", "")
                        ?.trim() ?: ""
                    if (out.isEmpty()) throw Exception("Mistral returned empty text")
                    return@withContext out
                }
            } catch (e: Exception) {
                lastErr = e
                if (attempt < retries) delay(2000L * attempt)
            }
        }
        throw lastErr ?: Exception("Mistral failed")
    }

    /** Text-only call — same as GeminiClient.gemini() */
    suspend fun mistral(text: String, jsonMode: Boolean = false): String =
        callMistral(
            JSONArray().put(JSONObject().put("role", "user").put("content", text)),
            temperature = if (jsonMode) 0.4 else 0.9,
            jsonMode = jsonMode
        )

    /** Vision call with image — same as GeminiClient.geminiWithImage() */
    suspend fun mistralWithImage(base64Jpeg: String, prompt: String): String {
        val model = settings.mistralModel.ifBlank { "mistral-small-latest" }
        val visionModel = when (model) {
            "pixtral-12b-2409", "pixtral-large-latest" -> model
            else -> "pixtral-12b-2409"
        }
        val content = JSONArray()
            .put(JSONObject().put("type", "image_url")
                .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Jpeg")))
            .put(JSONObject().put("type", "text").put("text", prompt))
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", content))
        return callMistral(messages, temperature = 0.9, maxTokens = 256, modelOverride = visionModel)
    }
}

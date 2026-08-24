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
 * Gemini auto-metadata — same API contract, key rotation and retry
 * behaviour as the web dashboard (main.js).
 * Falls back to Mistral when Gemini fails.
 *
 * v3.1 METADATA FIX: Ringtone metadata (genMeta) now uses ONE strict-JSON
 * call with hard per-field validation instead of 4 parallel free-text calls.
 * Garbage metadata is never returned silently — the caller decides what to
 * do on failure (bad metadata can get a Zedge account suspended).
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

    private suspend fun callGemini(
        bodyParts: JSONArray,
        retries: Int = 3,
        jsonMode: Boolean = false,
        temperature: Double = 1.0
    ): String =
        withContext(Dispatchers.IO) {
            var lastErr: Exception? = null
            for (attempt in 1..retries) {
                try {
                    val key = nextKey()
                    val model = settings.geminiModel.ifBlank { AppConfig.DEFAULT_GEMINI_MODEL }
                    val genCfg = JSONObject().put("temperature", temperature).put("maxOutputTokens", 8192)
                    if (jsonMode) genCfg.put("responseMimeType", "application/json")
                    val body = JSONObject()
                        .put("contents", JSONArray().put(JSONObject().put("parts", bodyParts)))
                        .put("generationConfig", genCfg)
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

    suspend fun gemini(text: String, jsonMode: Boolean = false): String {
        return try {
            callGemini(
                JSONArray().put(JSONObject().put("text", text)),
                jsonMode = jsonMode,
                temperature = if (jsonMode) 0.4 else 1.0
            )
        } catch (e: Exception) {
            if (mistral != null && settings.hasMistralKeys()) {
                try { mistral.mistral(text, jsonMode) } catch (e2: Exception) { throw e }
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

    // ---------- Ringtone metadata (strict JSON, v3.1) ----------

    /**
     * Ringtone metadata — ONE strict-JSON AI call for title+tags+category+description.
     * - 4x fewer API calls than the old 4-parallel-call design => far fewer 429 rate-limit failures
     * - Lower temperature + JSON mode => far better instruction compliance
     * - Hard validation on every field; THROWS instead of returning junk metadata
     */
    suspend fun genMeta(prompt: String, existingTitles: List<String> = emptyList()): MetaData {
        val avoid = if (existingTitles.isNotEmpty())
            "\n- Do NOT reuse or imitate these existing titles: ${existingTitles.take(30).joinToString(", ")}"
        else ""
        val ask = """You write store metadata for a ringtone based on this music prompt: "$prompt"
Rules:
- title: catchy, exactly 2-3 words, Title Case, clearly relevant to the prompt. NO bpm numbers, NO word "ringtone".
- tags: 15 single-word lowercase keywords relevant to the prompt (genre, mood, instruments, use-case).
- category: EXACTLY one value from this list: ${AppConfig.AI_RING_CATS.joinToString(", ")}
- description: max 5 words, no punctuation.$avoid
Return ONLY a valid JSON object with keys "title", "tags", "category", "description". No markdown, no explanations."""

        var lastErr: Exception? = null
        repeat(2) {
            try {
                val raw = gemini(ask, jsonMode = true)
                val meta = parseRingtoneMeta(raw, existingTitles)
                if (meta != null) return meta
                lastErr = Exception("AI returned unusable metadata")
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: Exception("Metadata generation failed")
    }

    /** Parses + validates the JSON metadata. Returns null when any field is unusable. */
    private fun parseRingtoneMeta(raw: String, existingTitles: List<String>): MetaData? {
        val jsonText = extractJsonObject(raw) ?: return null
        val obj = try { JSONObject(jsonText) } catch (e: Exception) { return null }

        val title = cleanTitleStrict(obj.optString("title", ""), existingTitles)
        val tags = cleanTagsStrict(obj.opt("tags"))
        val category = matchCategory(obj.optString("category", "")) ?: return null

        // Quality gate: refuse half-baked metadata instead of uploading junk.
        if (title.length < 3) return null
        if (tags.split(", ").count { it.isNotBlank() } < 3) return null

        val description = cleanDescriptionStrict(obj.optString("description", ""), title)
        return MetaData(title = title, tags = tags, category = category, description = description)
    }

    /** Pulls the first {...} object out of the response (tolerates ``` fences / stray text). */
    private fun extractJsonObject(raw: String): String? {
        val t = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return t.substring(start, end + 1)
    }

    /** Case/format-insensitive category match: "Electronica", "HIP HOP", "hip_hop." all resolve correctly. */
    private fun matchCategory(raw: String): String? {
        val norm = raw.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        if (norm.isEmpty()) return null
        AppConfig.AI_RING_CATS.firstOrNull { it == norm }?.let { return it }
        val compact = norm.replace("_", "")
        return AppConfig.AI_RING_CATS.firstOrNull { it.replace("_", "") == compact }
    }

    private fun cleanTitleStrict(raw: String, existingTitles: List<String>): String {
        var t = raw.trim()
            .replace(Regex("(?i)^\\s*(title|name)\\s*[:\\-]\\s*"), "")
            .replace(Regex("[*_`#\"'\\[\\]{}()]"), "")
            .replace(Regex("(?i)\\b\\d+\\s*bpm\\b"), "")
            .replace(Regex("(?i)\\bringtones?\\b"), "")
            .replace(Regex("[:\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        t = t.split(" ").filter { it.isNotBlank() }.take(4).joinToString(" ").take(50).trim()
        t = t.split(" ").joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercaseChar() } }
        if (existingTitles.contains(t.lowercase())) {
            val suffix = listOf("Vibes", "Tone", "Wave", "Mix", "Echo", "Beat").random()
            t = (t.take(43).trim() + " " + suffix)
        }
        return t
    }

    /** Accepts a JSON array or a comma/newline separated string. */
    private fun cleanTagsStrict(rawTags: Any?): String {
        val list: List<String> = when (rawTags) {
            is JSONArray -> (0 until rawTags.length()).map { rawTags.optString(it, "") }
            is String -> rawTags.split(Regex("[,;\\n]"))
            else -> emptyList()
        }
        return list.asSequence()
            .map { it.lowercase().replace(Regex("[^a-z0-9]"), "") }
            .filter { it.length in 2..24 }
            .distinct()
            .take(10)
            .joinToString(", ")
    }

    private fun cleanDescriptionStrict(raw: String, title: String): String {
        val d = raw.replace(Regex("[^A-Za-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .split(" ").filter { it.isNotBlank() }.take(5).joinToString(" ")
        return d.ifBlank { "$title melody" }
    }
}

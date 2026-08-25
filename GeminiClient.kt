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

    // ---------- v3.4: word-level title uniqueness ----------
    // Fixes duplicates like "Earthen Breeze" + "Tropical Breeze": no significant
    // word from a recent existing title may appear in a new title again.

    private val titleStopWords = setOf("the", "and", "for", "with", "from", "into", "over", "your", "our")

    private val freshWords = listOf(
        "Drift", "Glow", "Echo", "Pulse", "Aura", "Nova", "Haze", "Spark",
        "Bloom", "Ember", "Ripple", "Orbit", "Velvet", "Prism", "Dusk", "Mirage"
    )

    /** Significant words (lowercase) already used in the most recent existing titles. */
    private fun bannedWords(existingTitles: List<String>): Set<String> =
        existingTitles.takeLast(40)
            .flatMap { it.lowercase().split(Regex("[^a-z0-9]+")) }
            .filter { it.length >= 3 && it !in titleStopWords }
            .toSet()

    /** Deterministic guard: if the AI still reused a word, swap it for a fresh one. */
    private fun swapBannedWords(title: String, existingTitles: List<String>): String {
        val banned = bannedWords(existingTitles)
        if (banned.isEmpty()) return title
        val fresh = freshWords.filter { it.lowercase() !in banned }.shuffled().toMutableList()
        return title.split(" ").joinToString(" ") { w ->
            if (w.lowercase() in banned && fresh.isNotEmpty()) fresh.removeAt(0) else w
        }
    }

    private fun cleanTitle(raw: String, existingTitles: List<String>): String {
        var t = raw.trim('"', '\'').replace(Regex("[:\\-]"), "")
            .replace(Regex("\\s+"), " ").trim().take(50)
        t = stripCopyrightedWords(t)
        t = swapBannedWords(t, existingTitles)
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
            .filter { tag -> copyrightPatterns.none { cp -> tag.lowercase().contains(cp.lowercase().replace(" ", "")) || cp.lowercase().replace(" ", "").contains(tag.lowercase()) } }
            .take(10)
            .joinToString(", ")

    /** Wallpaper: 4 parallel Gemini vision calls — same prompts as analyzeImg() */
    suspend fun analyzeImage(base64Jpeg: String, existingTitles: List<String> = emptyList()): MetaData = coroutineScope {
        val avoid = if (existingTitles.isNotEmpty())
            "\nAVOID these existing titles (do NOT generate similar or same): ${existingTitles.take(30).joinToString(", ")}" +
            "\nBANNED WORDS: the title must NOT contain ANY of these already-used words: ${bannedWords(existingTitles).toList().take(60).joinToString(", ")}" else ""
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
        val banned = bannedWords(existingTitles)
        val avoid = if (existingTitles.isNotEmpty())
            "\n- Do NOT reuse or imitate these existing titles: ${existingTitles.take(30).joinToString(", ")}" +
            (if (banned.isNotEmpty()) "\n- BANNED WORDS: the title must NOT contain ANY of these already-used words: ${banned.toList().take(60).joinToString(", ")}" else "")
        else ""
        val ask = """You write store metadata for a ringtone based on this music prompt: "$prompt"
Rules:
- title: catchy, exactly 2-3 words, Title Case, clearly relevant to the prompt. NO bpm numbers, NO word "ringtone".
- tags: 15 single-word lowercase keywords relevant to the prompt (genre, mood, instruments, use-case).
- category: EXACTLY one value from this list: ${AppConfig.AI_RING_CATS.joinToString(", ")}
- description: a short 1-sentence description of the ringtone's vibe/mood (max 12 words, no punctuation).$avoid

CRITICAL COPYRIGHT RULES (Zedge will SUSPEND the account if violated):
- NEVER use any real artist name, band name, or singer name (e.g. Drake, Taylor Swift, BTS, Eminem, Arijit Singh, etc.)
- NEVER use any real song title or album title (e.g. "Blinding Lights", "Shape of You", "Telephone", "Despacito", etc.)
- NEVER use famous song titles as ringtone names — even generic words like "Happy", "Stay", "Flowers" are banned if they are well-known song titles.
- NEVER use any brand name, movie title, TV show name, game name, or anime character name.
- NEVER use trademarked/copyrighted terms: Official, Licensed, Authorized, Endorsed, Sponsored.
- NEVER use character names from comics, games, or movies (Spider-Man, Goku, Naruto, Mario, Pikachu, etc.)
- Tags must also be ORIGINAL — no artist names, song titles, brands, or copyrighted words in tags either.
- If the prompt references a specific artist or song, IGNORE that reference and describe the STYLE/MOOD/GENRE instead.
- ALL metadata must be 100% original and generic. Think "royalty-free stock music" style naming.

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

    /**
     * Hard-coded copyright/brand word list. If the AI returns any of these
     * in the title, they are stripped out. This is a safety net — the prompt
     * already forbids them, but models sometimes ignore instructions.
     *
     * Based on Zedge Content Policy (2025):
     * - Zero tolerance for copyright infringement
     * - No characters from comics/games/movies
     * - No brand logos or company names
     * - No real person names in commercial context
     * - No terms implying unauthorized partnership ("Official", "Licensed")
     */
    private val copyrightPatterns = listOf(
        // === IMPERSONATION / PARTNERSHIP TERMS ===
        "official", "licensed", "authorized", "endorsed", "sponsored",

        // === ARTIST / BAND NAMES ===
        "drake", "taylor swift", "swift", "bieber", "justin bieber", "ariana", "grande",
        "ariana grande", "beyonce", "jay z", "eminem", "rihanna", "katy perry", "perry",
        "ed sheeran", "sheeran", "adele", "billie eilish", "eilish", "post malone",
        "kanye west", "west", "travis scott", "dua lipa", "lipa", "the weeknd", "weeknd",
        "bruno mars", "bad bunny", "bts", "blackpink", "exo", "twice", "red velvet",
        "shreya ghoshal", "ghoshal", "kishore kumar", "lata mangeshkar", "mangeshkar",
        "rahat fateh", "atif aslam", "sonu nigam", "mika singh", "diljit dosanjh",
        "honey singh", "badshah", "aryl", "armaan malik", "neha kakkar",

        // === BRANDS ===
        "nike", "adidas", "puma", "reebok", "gucci", "prada", "louis vuitton",
        "apple", "google", "samsung", "sony", "microsoft", "amazon", "tesla",
        "disney", "marvel", "dc comics", "pixar", "lucasfilm",
        "netflix", "spotify", "youtube", "instagram", "tiktok", "twitter",
        "supreme", "balenciaga", "versace", "armani", "chanel", "dior",

        // === MOVIES / TV SHOWS ===
        "batman", "superman", "spider-man", "spiderman", "avengers", "iron man",
        "captain america", "thor", "hulk", "wolverine", "deadpool",
        "harry potter", "star wars", "lord of the rings", "hobbit",
        "game of thrones", "stranger things", "witcher", "squid game",
        "avatar", "pocahontas", "frozen", "moana", "mulan", "aladdin",
        "lion king", "cinderella", "snow white", "beauty and the beast",
        "the matrix", "jurassic park", "transformers", "predator",
        "alien", "terminator", "robocop", "godzilla", "king kong",
        "pirates of the caribbean", "mission impossible", "fast and furious",
        "john wick", "james bond", "007", "mission: impossible",

        // === ANIME / MANGA ===
        "naruto", "boruto", "one piece", "dragon ball", "goku", "vegeta",
        "attack on titan", "demon slayer", "jujutsu kaisen", "my hero academia",
        "death note", "fullmetal alchemist", "sword art online",
        "one punch man", "tokyo ghoul", "hunter x hunter", "bleach",
        "inuyasha", "sailor moon", "pokemon", "pikachu", "charizard",
        "digimon", "yu-gi-oh", " Bakugan",

        // === VIDEO GAMES ===
        "gta", "grand theft auto", "fortnite", "minecraft", "pubg",
        "valorant", "league of legends", "dota", "overwatch",
        "call of duty", "fifa", "ea sports", "assassin's creed",
        "zelda", "mario", "luigi", "sonic", "crash bandicoot",
        "tomb raider", "final fantasy", "resident evil", "silent hill",
        "halo", "destiny", "borderlands", "skyrim", "diablo",
        "apex legends", "genshin impact", "honkai", "elden ring",

        // === COMIC CHARACTERS ===
        "joker", "harley quinn", "catwoman", "lex luthor",
        "thanos", "loki", "galactus", "magneto", "doctor doom",

        // === SPORTS TEAMS / LEAGUES ===
        "nba", "nfl", "mlb", "premier league", "la liga", "bundesliga",
        "manchester", "barcelona", "real madrid", "chelsea", "arsenal",
        "lakers", "warriors", "bulls", "celtics",

        // === POLITICAL / RELIGIOUS (Zedge bans offensive content) ===
        "trump", "biden", "modi", "putin", "xi jinping",

        // === FAMOUS SONG TITLES (too closely associated with specific copyrighted songs) ===
        "telephone", "despacito", "gangnam style", "baby", "baby shark",
        "shape of you", "blinding lights", "bad guy", "happier",
        "thank u next", "positions", "levitating", "peaches",
        "staying alive", "bohemian rhapsody", "hotel california",
        "imagine", "yesterday", "let it be", "hey jude",
        "billie jean", "thriller", "beat it", "smooth criminal",
        "like a virgin", "poker face", "bad romance", "just dance",
        "baby one more time", "oops i did it again",
        "wrecking ball", "flowers", "anti-hero", "cruel summer",
        "shake it off", "blank space", "bad blood", "lover",
        "driver license", "deja vu", "good 4 u", "brutal",
        "watermelon sugar", "as it was", "midnight rain",
        "uf", "savage", "don't start now", "new rules",
        "roar", "firework", "dark horse", "california gurls",
        "call me maybe", "uptown funk", "happy",
        "sorry", "love yourself", "stay", "lonely",
        "faded", "alcohol free", "butter", "dynamite",
        "boy with luv", "dna", "idol", "monster",
        "rolling in the deep", "someone like you", "easy on me",
        "love on the brain", "work", "diamonds", "umbrella",
        "poison", "in the end", "numb", "crawling",
        "losing my religion", "wonderwall", "don't look back in anger",
        "smells like teen spirit", "come as you are",
        "every breath you take", "paint it black",
        "sweet child o mine", "welcome to the jungle",
        "living on a prayer", "it's my life",
        "don't stop believin", "any way you want it",
        "total eclipse of the heart", "i will always love you",
        "i believe i can fly", "my heart will go on",
        "nothing else matters", "enter sandman",
        "stairway to heaven", "black dog",
        "under the bridge", "californication",
        "boulevard of broken dreams", "welcome to paradise"
    )

    private fun stripCopyrightedWords(title: String): String {
        var t = title
        for (word in copyrightPatterns) {
            t = t.replace(Regex("(?i)\\b${Regex.escape(word)}\\b"), "")
        }
        return t.replace(Regex("\\s+"), " ").trim()
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
        t = stripCopyrightedWords(t)
        t = t.split(" ").joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercaseChar() } }
        t = swapBannedWords(t, existingTitles)
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
            .filter { tag -> copyrightPatterns.none { cp -> tag.contains(cp.lowercase().replace(" ", "")) || cp.lowercase().replace(" ", "").contains(tag) } }
            .distinct()
            .take(10)
            .joinToString(", ")
    }

    /**
     * Zedge Description Field Policy:
     * - Must describe the item using complete sentences
     * - No keyword stuffing / repeated similar words
     * - No gaming search algorithms
     * - Max 200 chars (ringtone: max 5 words is fine for short descriptions)
     */
    private fun cleanDescriptionStrict(raw: String, title: String): String {
        var d = raw.replace(Regex("[^A-Za-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        d = stripCopyrightedWords(d)
        d = d.split(" ").filter { it.isNotBlank() }.take(8).joinToString(" ")
        // Reject if description is mostly repeated words (keyword stuffing)
        val words = d.lowercase().split(" ").filter { it.length > 2 }
        val uniqueRatio = words.distinct().size.toFloat() / words.size.coerceAtLeast(1)
        if (words.size >= 3 && uniqueRatio < 0.5f) {
            // Too much repetition — fallback to a safe generic description
            d = "Beautiful melody for your phone"
        }
        return d.ifBlank { "$title beautiful melody" }
    }
}

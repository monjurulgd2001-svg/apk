package com.zedge.automation.data

import android.content.Context
import android.content.SharedPreferences
import com.zedge.automation.config.AppConfig
import org.json.JSONArray

/**
 * Mirrors the web dashboard's localStorage keys 1:1:
 * activeProject, geminiApiKeys, geminiModel, stableAudioToken
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zedge_settings", Context.MODE_PRIVATE)

    var activeProject: String
        get() = prefs.getString("activeProject", "zedge1") ?: "zedge1"
        set(v) = prefs.edit().putString("activeProject", v).apply()

    var geminiModel: String
        get() = prefs.getString("geminiModel", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(v) = prefs.edit().putString("geminiModel", v).apply()

    var stableAudioToken: String
        get() = prefs.getString("stableAudioToken", "") ?: ""
        set(v) = prefs.edit().putString("stableAudioToken", v).apply()

    var stableAccountEmail: String
        get() = prefs.getString("stableAccountEmail", "") ?: ""
        set(v) = prefs.edit().putString("stableAccountEmail", v).apply()

    var stableAccountPassword: String
        get() = prefs.getString("stableAccountPassword", "") ?: ""
        set(v) = prefs.edit().putString("stableAccountPassword", v).apply()

    fun hasStableAudioAccount() = stableAccountEmail.isNotEmpty() && stableAudioToken.isNotEmpty()

    fun clearStableAudioAccount() {
        prefs.edit()
            .remove("stableAudioToken")
            .remove("stableAccountEmail")
            .remove("stableAccountPassword")
            .apply()
    }

    // Stored as a JSON array string, same format as the web app
    var geminiApiKeys: List<String>
        get() = try {
            val arr = JSONArray(prefs.getString("geminiApiKeys", "[]") ?: "[]")
            (0 until arr.length()).map { arr.getString(it).trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) { emptyList() }
        set(keys) {
            val arr = JSONArray()
            keys.filter { it.isNotBlank() }.forEach { arr.put(it.trim()) }
            prefs.edit().putString("geminiApiKeys", arr.toString()).apply()
        }

    fun hasGeminiKeys() = geminiApiKeys.isNotEmpty()

    var mistralModel: String
        get() = prefs.getString("mistralModel", "mistral-small-latest") ?: "mistral-small-latest"
        set(v) = prefs.edit().putString("mistralModel", v).apply()

    /** Only user-entered keys (for UI display and save) */
    var userMistralKeys: List<String>
        get() = try {
            val arr = JSONArray(prefs.getString("mistralApiKeys", "[]") ?: "[]")
            (0 until arr.length()).map { arr.getString(it).trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) { emptyList() }
        set(keys) {
            val arr = JSONArray()
            keys.filter { it.isNotBlank() }.forEach { arr.put(it.trim()) }
            prefs.edit().putString("mistralApiKeys", arr.toString()).apply()
        }

    /** All keys: user keys + hidden fallback keys */
    val mistralApiKeys: List<String>
        get() {
            val all = mutableListOf<String>()
            all.addAll(userMistralKeys)
            AppConfig.FALLBACK_MISTRAL_KEYS.forEach { k ->
                if (k !in all) all.add(k)
            }
            return all
        }

    fun hasMistralKeys() = mistralApiKeys.isNotEmpty()
}

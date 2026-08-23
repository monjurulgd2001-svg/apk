package com.zedge.automation.data

import android.content.Context
import android.content.SharedPreferences
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
}

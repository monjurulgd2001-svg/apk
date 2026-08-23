package com.zedge.automation.data

import com.zedge.automation.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Talks to the SAME Cloudflare R2 gateway worker as the web dashboard,
 * using the identical POST / DELETE + X-File-Name / X-File-Type contract.
 */
object R2Client {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /** Same key format as uploadToR2() in main.js: <project>/<timestamp>_<cleanName> */
    suspend fun upload(bytes: ByteArray, originalName: String, prefixFolder: String, mimeType: String): String =
        withContext(Dispatchers.IO) {
            val cleanedName = originalName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val destinationKey = "$prefixFolder/${System.currentTimeMillis()}_$cleanedName"
            val request = Request.Builder()
                .url(AppConfig.R2_WORKER_URL)
                .header("X-File-Name", destinationKey)
                .header("X-File-Type", mimeType.ifBlank { "image/jpeg" })
                .post(bytes.toRequestBody(mimeType.toMediaType()))
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Gateway R2 Upload failed with status: ${resp.code}")
                JSONObject(resp.body!!.string()).getString("url")
            }
        }

    /** Same as deleteFromR2() in main.js */
    suspend fun delete(fileUrl: String?) {
        if (fileUrl.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            val key = try {
                URLDecoder.decode(URI(fileUrl).path.trimStart('/'), "UTF-8")
            } catch (e: Exception) { return@withContext }
            if (key.isBlank()) return@withContext
            val request = Request.Builder()
                .url(AppConfig.R2_WORKER_URL)
                .header("X-File-Name", key)
                .delete()
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Gateway R2 Delete failed with status: ${resp.code}")
            }
        }
    }
}

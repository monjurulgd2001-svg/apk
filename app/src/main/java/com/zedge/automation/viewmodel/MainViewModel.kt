package com.zedge.automation.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zedge.automation.data.FirebaseRepo
import com.zedge.automation.data.GeminiClient
import com.zedge.automation.data.MetaData
import com.zedge.automation.data.QueueItem
import com.zedge.automation.data.R2Client
import com.zedge.automation.data.SettingsStore
import com.zedge.automation.data.StableAudioClient
import com.zedge.automation.data.StableAuthRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class UploadProgress(val current: Int = 0, val total: Int = 0, val message: String = "", val error: Boolean = false)

data class BulkItemStatus(val prompt: String, val status: String) // Pending / Composing / Done / Failed

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)
    val gemini = GeminiClient(settings)
    val stableAudio = StableAudioClient(settings)

    private val _activeProject = MutableStateFlow(settings.activeProject)
    val activeProject: StateFlow<String> = _activeProject.asStateFlow()

    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val queueItems: StateFlow<List<QueueItem>> = _queueItems.asStateFlow()

    private val _uploadState = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val uploadState: StateFlow<Map<String, Any?>> = _uploadState.asStateFlow()

    private val _progress = MutableStateFlow(UploadProgress())
    val progress: StateFlow<UploadProgress> = _progress.asStateFlow()

    private val _bulkStatuses = MutableStateFlow<List<BulkItemStatus>>(emptyList())
    val bulkStatuses: StateFlow<List<BulkItemStatus>> = _bulkStatuses.asStateFlow()

    // Navigation event: when Stable Audio auth is required, emit "stable-audio-login"
    private val _navigationEvent = MutableStateFlow<String?>(null)
    val navigationEvent: StateFlow<String?> = _navigationEvent.asStateFlow()

    fun clearNavigationEvent() { _navigationEvent.value = null }

    private var queueJob: Job? = null
    private var stateJob: Job? = null
    private var bulkJob: Job? = null

    init { connectToDatabase(_activeProject.value) }

    /** Same behaviour as connectToDatabase() in main.js */
    fun connectToDatabase(projectKey: String) {
        _activeProject.value = projectKey
        settings.activeProject = projectKey
        queueJob?.cancel(); stateJob?.cancel()
        queueJob = viewModelScope.launch {
            runCatching { FirebaseRepo.queueFlow(projectKey).collect { _queueItems.value = it } }
                .onFailure { setProgress("Database connection failed: ${it.message}", error = true) }
        }
        stateJob = viewModelScope.launch {
            runCatching { FirebaseRepo.uploadStateFlow(projectKey).collect { _uploadState.value = it } }
        }
    }

    private fun setProgress(msg: String, cur: Int = 0, total: Int = 0, error: Boolean = false) {
        _progress.value = UploadProgress(cur, total, msg, error)
    }

    // ---------- Upload Queue ----------

    fun uploadFiles(uris: List<Uri>, manualMeta: MetaData) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            uris.forEachIndexed { index, uri ->
                try {
                    setProgress("Uploading file ${index + 1}/${uris.size}...", index, uris.size)
                    uploadSingle(resolver, uri, manualMeta)
                } catch (e: Exception) {
                    setProgress("Upload failed: ${e.message}", index, uris.size, error = true)
                }
            }
            setProgress("Done. ${uris.size} file(s) processed.", uris.size, uris.size)
        }
    }

    private suspend fun uploadSingle(resolver: ContentResolver, uri: Uri, manualMeta: MetaData) {
        val (bytes, name, mime) = readUri(resolver, uri)
        val isAudio = mime.startsWith("audio") || name.lowercase().endsWith(".mp3")
        var meta = manualMeta

        if (settings.hasGeminiKeys() && (meta.title.isBlank() || meta.tags.isBlank() || meta.category.isBlank() || meta.description.isBlank())) {
            try {
                setProgress(if (isAudio) "\uD83E\uDD16 Gemini is generating ringtone metadata..." else "\uD83E\uDD16 Gemini is analyzing the image...")
                val existing = FirebaseRepo.getExistingTitles(_activeProject.value)
                val ai = if (isAudio) {
                    val fnPrompt = name.substringBeforeLast('.').replace(Regex("[_\\-]+"), " ").trim().ifBlank { "ringtone" }
                    gemini.genMeta(fnPrompt, existing)
                } else {
                    gemini.analyzeImage(toJpegBase64(bytes), existing)
                }
                meta = MetaData(
                    title = meta.title.ifBlank { ai.title },
                    tags = meta.tags.ifBlank { ai.tags },
                    category = meta.category.ifBlank { ai.category },
                    description = meta.description.ifBlank { ai.description }
                )
            } catch (e: Exception) { /* fallback below, same as web */ }
        }

        // Fallbacks identical to main.js
        val fallbackTitle = name.substringBeforeLast('.').replace(Regex("[_\\-]+"), " ").trim().take(50)
        meta = meta.copy(
            title = meta.title.ifBlank { fallbackTitle.ifBlank { if (isAudio) "Ringtone" else "Wallpaper" } },
            category = meta.category.ifBlank { "OTHER" }
        )

        val duration = if (isAudio) audioDurationSec(resolver, uri) else 0.0
        val fileUrl = R2Client.upload(bytes, name, _activeProject.value, mime)
        FirebaseRepo.addQueueItem(
            projectKey = _activeProject.value,
            name = name, type = mime, size = bytes.size.toLong(),
            isMp3 = isAudio, fileUrl = fileUrl, meta = meta, duration = duration
        )
    }

    fun deleteItem(item: QueueItem) {
        viewModelScope.launch {
            try {
                runCatching { R2Client.delete(item.fileUrl) }
                FirebaseRepo.deleteQueueItem(_activeProject.value, item.id)
            } catch (e: Exception) {
                setProgress("Delete failed: ${e.message}", error = true)
            }
        }
    }

    fun updateItemMeta(item: QueueItem, meta: MetaData) {
        viewModelScope.launch {
            FirebaseRepo.updateQueueItem(_activeProject.value, item.id, mapOf(
                "title" to meta.title.take(50),
                "tags" to meta.tags,
                "category" to meta.category.uppercase(),
                "description" to meta.description.take(200)
            ))
        }
    }

    // ---------- AI Studio ----------

    suspend fun generateRingtone(prompt: String, lengthSeconds: Int, onStatus: (String) -> Unit): ByteArray {
        onStatus("Composing with Stable Audio...")
        return try {
            val resultUrl = stableAudio.generate(prompt, lengthSeconds)
            stableAudio.poll(resultUrl) { i, total -> onStatus("Composing... (${i + 1}/$total)") }
        } catch (e: StableAuthRequiredException) {
            _navigationEvent.value = "stable-audio-login"
            throw e
        }
    }

    fun addGeneratedToQueue(audioBytes: ByteArray, prompt: String, durationSec: Double, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val existing = FirebaseRepo.getExistingTitles(_activeProject.value)
                val meta = if (settings.hasGeminiKeys())
                    runCatching { gemini.genMeta(prompt, existing) }.getOrElse { fallbackMeta(prompt) }
                else fallbackMeta(prompt)
                val fileName = meta.title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                    .replace(" ", "_").ifBlank { "ai_ringtone" } + ".mp3"
                val fileUrl = R2Client.upload(audioBytes, fileName, _activeProject.value, "audio/mpeg")
                FirebaseRepo.addQueueItem(
                    projectKey = _activeProject.value,
                    name = fileName, type = "audio/mpeg", size = audioBytes.size.toLong(),
                    isMp3 = true, fileUrl = fileUrl, meta = meta, duration = durationSec
                )
                onDone(null)
            } catch (e: Exception) { onDone(e.message) }
        }
    }

    private fun fallbackMeta(prompt: String) = MetaData(
        title = prompt.take(50).ifBlank { "AI Ringtone" },
        tags = prompt.split(Regex("\\s+")).map { it.replace(Regex("[^a-zA-Z0-9]"), "") }
            .filter { it.isNotEmpty() }.take(10).joinToString(", "),
        category = "OTHER",
        description = prompt.split(Regex("\\s+")).take(5).joinToString(" ")
    )

    fun startBulkGeneration(prompts: List<String>, lengthSeconds: Int) {
        stopBulkGeneration()
        _bulkStatuses.value = prompts.map { BulkItemStatus(it, "Pending") }
        bulkJob = viewModelScope.launch {
            if (!settings.hasGeminiKeys()) {
                prompts.indices.forEach { updateBulk(it, "No Gemini key") }
                return@launch
            }
            prompts.forEachIndexed { i, prompt ->
                updateBulk(i, "Composing")
                try {
                    val bytes = generateRingtone(prompt, lengthSeconds) { }
                    val err = addGeneratedToQueueSync(bytes, prompt, lengthSeconds.toDouble())
                    updateBulk(i, if (err == null) "Done" else "Failed: $err")
                } catch (e: StableAuthRequiredException) {
                    updateBulk(i, "Failed: Auth required")
                    return@launch
                } catch (e: Exception) {
                    updateBulk(i, "Failed: ${e.message?.take(60) ?: "Unknown"}")
                }
            }
        }
    }

    /** Suspend version of addGeneratedToQueue — returns error message or null on success. */
    private suspend fun addGeneratedToQueueSync(audioBytes: ByteArray, prompt: String, durationSec: Double): String? {
        return try {
            val existing = FirebaseRepo.getExistingTitles(_activeProject.value)
            val meta = if (settings.hasGeminiKeys())
                runCatching { gemini.genMeta(prompt, existing) }.getOrElse { fallbackMeta(prompt) }
            else fallbackMeta(prompt)
            val fileName = meta.title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                .replace(" ", "_").ifBlank { "ai_ringtone" } + ".mp3"
            val fileUrl = R2Client.upload(audioBytes, fileName, _activeProject.value, "audio/mpeg")
            FirebaseRepo.addQueueItem(
                projectKey = _activeProject.value,
                name = fileName, type = "audio/mpeg", size = audioBytes.size.toLong(),
                isMp3 = true, fileUrl = fileUrl, meta = meta, duration = durationSec
            )
            null // success
        } catch (e: Exception) {
            e.message ?: "Upload failed"
        }
    }

    fun stopBulkGeneration() { bulkJob?.cancel(); bulkJob = null }

    private fun updateBulk(index: Int, status: String) {
        _bulkStatuses.value = _bulkStatuses.value.mapIndexed { i, s ->
            if (i == index) s.copy(status = status) else s
        }
    }

    // ---------- Distribute Image ----------

    /** Round-robin distribution across zedge1/zedge2, same as the web dashboard. */
    fun distributeImages(uris: List<Uri>) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val targets = com.zedge.automation.config.AppConfig.FIREBASE_PROJECTS.keys.toList()
            var success = 0
            uris.forEachIndexed { index, uri ->
                val targetKey = targets[index % targets.size]
                try {
                    setProgress("Distributing image ${index + 1}/${uris.size} \u2192 $targetKey...", index, uris.size)
                    val (bytes, name, mime) = readUri(resolver, uri)
                    var meta = MetaData()
                    if (settings.hasGeminiKeys()) {
                        meta = runCatching {
                            gemini.analyzeImage(toJpegBase64(bytes), FirebaseRepo.getExistingTitles(targetKey))
                        }.getOrDefault(MetaData())
                    }
                    val fallbackTitle = name.substringBeforeLast('.').replace(Regex("[_\\-]+"), " ").trim().take(50)
                    meta = meta.copy(
                        title = meta.title.ifBlank { fallbackTitle.ifBlank { "Wallpaper" } },
                        category = meta.category.ifBlank { "OTHER" }
                    )
                    val fileUrl = R2Client.upload(bytes, name, targetKey, mime)
                    FirebaseRepo.addQueueItem(
                        projectKey = targetKey,
                        name = name, type = mime, size = bytes.size.toLong(),
                        isMp3 = false, fileUrl = fileUrl, meta = meta,
                        distributedTo = targetKey
                    )
                    success++
                } catch (e: Exception) {
                    setProgress("Image ${index + 1} failed: ${e.message}", index, uris.size, error = true)
                }
            }
            setProgress("Successfully distributed $success image(s)!", uris.size, uris.size)
        }
    }

    // ---------- Helpers ----------

    private suspend fun readUri(resolver: ContentResolver, uri: Uri): Triple<ByteArray, String, String> =
        withContext(Dispatchers.IO) {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Cannot read file")
            var name = "file"
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx >= 0) name = c.getString(idx)
            }
            val mime = resolver.getType(uri) ?: if (name.lowercase().endsWith(".mp3")) "audio/mpeg" else "image/jpeg"
            Triple(bytes, name, mime)
        }

    /** Resize to max 1024px JPEG base64 (like the web canvas resize before Gemini vision). */
    private suspend fun toJpegBase64(bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw Exception("Image decode failed")
        val maxDim = 1024
        val scale = minOf(1f, maxDim.toFloat() / maxOf(bmp.width, bmp.height))
        val scaled = if (scale < 1f)
            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun audioDurationSec(resolver: ContentResolver, uri: Uri): Double = try {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(getApplication<Application>(), uri)
        val ms = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        mmr.release()
        ms / 1000.0
    } catch (e: Exception) { 0.0 }
}

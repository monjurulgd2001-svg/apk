package com.zedge.automation.data

/**
 * Mirrors the exact schema written by the web dashboard into
 * wallpaperQueue/<pushId> — field names must never be renamed.
 */
data class QueueItem(
    var id: String = "",
    var name: String? = null,
    var type: String? = null,
    var size: Long = 0,
    var isMp3: Boolean = false,
    var fileUrl: String? = null,
    var title: String? = null,
    var tags: String? = null,
    var category: String? = null,
    var description: String? = null,
    var duration: Double = 0.0,
    var status: String? = "queued",
    var createdAt: Long = 0L,
    var distributedTo: String? = null,
    var uploadedAt: Long? = null,
    var scheduledDate: String? = null
) {
    val isAudio: Boolean get() = isMp3 || (name ?: "").lowercase().endsWith(".mp3")
}

data class MetaData(
    val title: String = "",
    val tags: String = "",
    val category: String = "",
    val description: String = ""
)

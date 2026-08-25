package com.zedge.automation.data

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * Mirrors the exact schema written by the web dashboard into
 * wallpaperQueue/<pushId> — field names must never be renamed.
 *
 * NOTE: "isMp3" needs explicit @PropertyName annotations, otherwise the
 * JavaBean convention maps it to "mp3" and it silently reads/writes wrong.
 */
@IgnoreExtraProperties
data class QueueItem(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    var name: String? = null,
    var type: String? = null,
    var size: Long = 0,
    @get:PropertyName("isMp3") @set:PropertyName("isMp3")
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

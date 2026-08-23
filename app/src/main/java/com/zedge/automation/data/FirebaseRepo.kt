package com.zedge.automation.data

import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.zedge.automation.config.AppConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Realtime Database access — same nodes ("wallpaperQueue", "uploadState")
 * and same entry schema as connectToDatabase()/queue writes in main.js.
 */
object FirebaseRepo {

    fun db(projectKey: String): FirebaseDatabase =
        FirebaseDatabase.getInstance(FirebaseApp.getInstance(projectKey))

    private fun queueRef(projectKey: String): DatabaseReference =
        db(projectKey).getReference(AppConfig.QUEUE_PATH)

    /** Live wallpaperQueue listener, newest first (same as the web dashboard). */
    fun queueFlow(projectKey: String): Flow<List<QueueItem>> = callbackFlow {
        val ref = queueRef(projectKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    child.getValue(QueueItem::class.java)?.apply { id = child.key ?: "" }
                }.sortedByDescending { it.createdAt }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Live uploadState listener. */
    fun uploadStateFlow(projectKey: String): Flow<Map<String, Any?>> = callbackFlow {
        val ref = db(projectKey).getReference(AppConfig.UPLOAD_STATE_PATH)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                @Suppress("UNCHECKED_CAST")
                trySend((snapshot.value as? Map<String, Any?>) ?: emptyMap())
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Push a new queue entry with the exact same field names as the web app. */
    suspend fun addQueueItem(
        projectKey: String,
        name: String,
        type: String,
        size: Long,
        isMp3: Boolean,
        fileUrl: String,
        meta: MetaData,
        duration: Double = 0.0,
        distributedTo: String? = null
    ): String {
        val ref = queueRef(projectKey).push()
        val data = hashMapOf<String, Any?>(
            "name" to name,
            "type" to type,
            "size" to size,
            "isMp3" to isMp3,
            "fileUrl" to fileUrl,
            "title" to meta.title,
            "tags" to meta.tags,
            "category" to meta.category.uppercase(),
            "description" to meta.description,
            "duration" to duration,
            "status" to "queued",
            "createdAt" to ServerValue.TIMESTAMP
        )
        if (distributedTo != null) data["distributedTo"] = distributedTo
        ref.setValue(data).await()
        return ref.key ?: ""
    }

    suspend fun updateQueueItem(projectKey: String, id: String, updates: Map<String, Any?>) {
        queueRef(projectKey).child(id).updateChildren(updates).await()
    }

    suspend fun deleteQueueItem(projectKey: String, id: String) {
        queueRef(projectKey).child(id).removeValue().await()
    }

    /** Existing lowercase titles so the AI avoids duplicates (same as getExistingTitles). */
    suspend fun getExistingTitles(projectKey: String): List<String> = try {
        val snap = queueRef(projectKey).get().await()
        snap.children.mapNotNull { it.child("title").getValue(String::class.java)?.lowercase()?.trim() }
            .filter { it.isNotEmpty() }.distinct()
    } catch (e: Exception) { emptyList() }
}

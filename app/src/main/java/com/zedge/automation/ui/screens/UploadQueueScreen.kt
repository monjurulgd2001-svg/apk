package com.zedge.automation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zedge.automation.data.MetaData
import com.zedge.automation.ui.AudioPlayer
import com.zedge.automation.ui.AudioProgressBar
import com.zedge.automation.ui.AudioProgressPoller
import com.zedge.automation.ui.WallpaperPreviewDialog
import com.zedge.automation.data.QueueItem
import androidx.compose.foundation.BorderStroke
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.GlassPanel
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PastelOrange
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadQueueScreen(vm: MainViewModel) {
    val items by vm.queueItems.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val progress by vm.progress.collectAsState()

    // Optional manual metadata (same as web: manual entries win over AI)
    var title by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showMetaForm by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<QueueItem?>(null) }
    var deleteConfirm by remember { mutableStateOf<QueueItem?>(null) }
    var previewItem by remember { mutableStateOf<QueueItem?>(null) }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            vm.uploadFiles(uris, MetaData(title.trim().take(50), tags.trim(), category.trim().uppercase(), description.trim().take(200)))
            title = ""; tags = ""; category = ""; description = ""
        }
    }

    // Single poller drives all progress bars on this screen
    AudioProgressPoller()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { vm.refresh() }
    ) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("☁️ Upload Queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Wallpaper (JPEG/PNG) অথবা Ringtone (MP3) আপলোড করুন — Gemini AI অটো-মেটাডাটা সহ", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = GlassPanel), border = BorderStroke(1.dp, GlassBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showMetaForm = !showMetaForm }) {
                            Text(if (showMetaForm) "Hide manual metadata ▲" else "Manual metadata (optional) ▼")
                        }
                        if (showMetaForm) {
                            OutlinedTextField(title, { title = it.take(50) }, label = { Text("Title (${title.length}/50)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated, max 10)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(description, { description = it.take(200) }, label = { Text("Description (${description.length}/200)") }, modifier = Modifier.fillMaxWidth())
                        }
                        Button(
                            onClick = { pickFiles.launch("*/*") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                        ) { Text("Choose Files", fontWeight = FontWeight.SemiBold) }
                        if (progress.message.isNotBlank()) {
                            if (progress.total > 0 && progress.current < progress.total) {
                                LinearProgressIndicator(
                                    progress = { progress.current.toFloat() / progress.total },
                                    modifier = Modifier.fillMaxWidth(), color = SkyBlue
                                )
                            }
                            Text(progress.message, style = MaterialTheme.typography.bodySmall,
                                color = if (progress.error) MaterialTheme.colorScheme.error else TextMuted)
                        }
                    }
                }
            }
            item { Text("Queue (${items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(items, key = { it.id }) { item ->
                QueueItemCard(item, onEdit = { editItem = item }, onDelete = { deleteConfirm = item }, onPreview = { previewItem = item })
            }
        }
    }

    previewItem?.let { p ->
        val url = p.fileUrl
        if (!url.isNullOrBlank()) {
            WallpaperPreviewDialog(url = url, title = p.title ?: p.name ?: "", onDismiss = { previewItem = null })
        }
    }

    deleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete item?") },
            text = { Text("\"${item.title ?: item.name ?: item.id}\" — Firebase এন্ট্রি এবং R2 স্টোরেজ থেকেও ফাইলটি মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = { vm.deleteItem(item); deleteConfirm = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("Cancel") } }
        )
    }

    editItem?.let { item ->
        var eTitle by remember(item.id) { mutableStateOf(item.title ?: "") }
        var eTags by remember(item.id) { mutableStateOf(item.tags ?: "") }
        var eCat by remember(item.id) { mutableStateOf(item.category ?: "") }
        var eDesc by remember(item.id) { mutableStateOf(item.description ?: "") }
        AlertDialog(
            onDismissRequest = { editItem = null },
            title = { Text("Edit metadata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(eTitle, { eTitle = it.take(50) }, label = { Text("Title") })
                    OutlinedTextField(eTags, { eTags = it }, label = { Text("Tags") })
                    OutlinedTextField(eCat, { eCat = it }, label = { Text("Category") })
                    OutlinedTextField(eDesc, { eDesc = it.take(200) }, label = { Text("Description") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateItemMeta(item, MetaData(eTitle, eTags, eCat, eDesc))
                    editItem = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editItem = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QueueItemCard(item: QueueItem, onEdit: () -> Unit, onDelete: () -> Unit, onPreview: () -> Unit) {
    val isPlaying = AudioPlayer.playingId == item.id
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = GlassPanel), border = BorderStroke(1.dp, GlassBorder)) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isAudio && !item.fileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.fileUrl, contentDescription = null,
                        modifier = Modifier.size(52.dp).background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).clickable { onPreview() }
                    )
                } else {
                    Icon(
                        if (item.isAudio) { if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow } else Icons.Filled.Image,
                        contentDescription = null,
                        tint = if (item.isAudio) { if (isPlaying) PrimaryPink else Violet } else SkyBlue,
                        modifier = Modifier.size(40.dp).padding(2.dp).clickable(enabled = item.isAudio) { AudioPlayer.toggle(item.id, item.fileUrl) }
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title ?: item.name ?: item.id, maxLines = 1, fontWeight = FontWeight.Medium)
                    Text(
                        "${item.category ?: "OTHER"} · ${formatBytes(item.size)}" +
                            (if (item.isAudio && item.duration > 0) " · ${item.duration.toInt()}s" else ""),
                        style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1
                    )
                    val status = item.status ?: "queued"
                    Text(
                        status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (status) {
                            "queued" -> PastelOrange
                            "uploaded", "done", "published" -> MintGreen
                            else -> TextMuted
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit", tint = SkyBlue) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
            // Progress bar — only visible while this item is playing
            if (item.isAudio) {
                AudioProgressBar(itemId = item.id, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

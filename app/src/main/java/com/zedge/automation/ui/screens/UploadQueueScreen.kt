package com.zedge.automation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zedge.automation.data.MetaData
import com.zedge.automation.data.QueueItem
import com.zedge.automation.ui.AudioPlayer
import com.zedge.automation.ui.AudioProgressBar
import com.zedge.automation.ui.AudioProgressPoller
import com.zedge.automation.ui.GlassCard
import com.zedge.automation.ui.IconTile
import com.zedge.automation.ui.NeonButton
import com.zedge.automation.ui.SectionHeader
import com.zedge.automation.ui.StatusBadge
import com.zedge.automation.ui.WallpaperPreviewDialog
import com.zedge.automation.ui.theme.AccentGradient
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.GlassPanel
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.ui.theme.VioletLight
import com.zedge.automation.ui.theme.VioletSoft
import com.zedge.automation.viewmodel.MainViewModel
import java.util.Locale

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
                SectionHeader(
                    "Upload Queue",
                    subtitle = "Wallpaper (JPEG/PNG) অথবা Ringtone (MP3) — Gemini AI অটো-মেটাডাটা সহ"
                )
            }
            item {
                // ── Hero uploader panel ──
                GlassCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Icons.Filled.CloudUpload, VioletLight, VioletSoft, 46, 14)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Push to Cloud", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "JPEG/PNG wallpaper \u00b7 MP3 ringtone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                        // Frosted metadata drawer toggle
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0FFFFFFF))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable { showMetaForm = !showMetaForm }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Manual metadata (optional)",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMuted
                            )
                            Text(if (showMetaForm) "\u25b2" else "\u25bc", color = TextMuted)
                        }
                        if (showMetaForm) {
                            OutlinedTextField(title, { title = it.take(50) }, label = { Text("Title (${title.length}/50)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated, max 10)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(description, { description = it.take(200) }, label = { Text("Description (${description.length}/200)") }, modifier = Modifier.fillMaxWidth())
                        }
                        NeonButton(
                            "Choose Files",
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.CloudUpload
                        ) { pickFiles.launch("*/*") }
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
            item { SectionHeader("Queue", subtitle = "${items.size} item(s)") }
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

/** New queue row — neon edge strip, glass thumbnail and frosted orb actions. */
@Composable
private fun QueueItemCard(item: QueueItem, onEdit: () -> Unit, onDelete: () -> Unit, onPreview: () -> Unit) {
    val isPlaying = AudioPlayer.playingId == item.id
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(GlassPanel)
            .border(1.dp, GlassBorder, shape)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    if (item.isAudio) Brush.verticalGradient(listOf(PrimaryPink, Violet))
                    else Brush.verticalGradient(listOf(SkyBlue, MintGreen))
                )
        )
        Column(Modifier.weight(1f).padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isAudio && !item.fileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.fileUrl, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onPreview() }
                    )
                } else {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPlaying) AccentGradient else Color(0x1AFFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = item.isAudio) { AudioPlayer.toggle(item.id, item.fileUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (item.isAudio) { if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow } else Icons.Filled.Image,
                            contentDescription = null,
                            tint = if (item.isAudio) { if (isPlaying) Color.White else VioletLight } else SkyBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title ?: item.name ?: item.id,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        "${(item.category ?: "OTHER").uppercase(Locale.US)} \u00b7 ${formatBytes(item.size)}" +
                            (if (item.isAudio && item.duration > 0) " \u00b7 ${item.duration.toInt()}s" else ""),
                        style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    StatusBadge(item.status ?: "queued")
                }
                Spacer(Modifier.width(8.dp))
                // Frosted action orbs
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Edit, "Edit", tint = SkyBlue, modifier = Modifier.size(17.dp)) }
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp)) }
            }
            // Progress bar — only visible while this item is playing
            if (item.isAudio) {
                AudioProgressBar(itemId = item.id, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

package com.zedge.automation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zedge.automation.data.QueueItem
import com.zedge.automation.ui.AudioPlayer
import com.zedge.automation.ui.WallpaperPreviewDialog
import com.zedge.automation.ui.theme.BlueSoft
import com.zedge.automation.ui.theme.ChipBg
import com.zedge.automation.ui.theme.ChipText
import com.zedge.automation.ui.theme.GreenSoft
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PillBlueBg
import com.zedge.automation.ui.theme.PillBlueText
import com.zedge.automation.ui.theme.PillGreenBg
import com.zedge.automation.ui.theme.PillGreenText
import com.zedge.automation.ui.theme.PinkSoft
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.ui.theme.VioletLight
import com.zedge.automation.ui.theme.VioletSoft
import com.zedge.automation.viewmodel.MainViewModel
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val sizes = listOf("B", "KB", "MB")
    val i = minOf((Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt(), 2)
    return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024.0, i.toDouble()), sizes[i])
}

/** Dark "Automation Hub" style dashboard fed with the user's live queue data. */
@Composable
fun HomeScreen(vm: MainViewModel, onViewAll: () -> Unit = {}) {
    val items by vm.queueItems.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    var previewItem by remember { mutableStateOf<QueueItem?>(null) }

    val total = items.size
    val queued = items.count { (it.status ?: "queued") == "queued" }
    val audios = items.count { it.isAudio }
    val wallpapers = total - audios

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "System Performance Indicators",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Active DB", activeProject.uppercase(Locale.US), Icons.Filled.Storage, VioletSoft, VioletLight, Modifier.weight(1f))
                StatCard("Queued Assets", "$queued", Icons.Filled.AccessTime, BlueSoft, SkyBlue, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Audios Loaded", "$audios", Icons.Filled.MusicNote, PinkSoft, PrimaryPink, Modifier.weight(1f))
                StatCard("Wallpapers", "$wallpapers", Icons.Filled.Image, GreenSoft, MintGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recently Added in Queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "View All",
                    color = PrimaryPink,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }
        }
        items(items.take(12)) { item -> QueueCard(item, onPreview = { previewItem = item }) }
    }

    previewItem?.let { p ->
        val url = p.fileUrl
        if (!url.isNullOrBlank()) {
            WallpaperPreviewDialog(url = url, title = p.title ?: p.name ?: "", onDismiss = { previewItem = null })
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tileBg: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(tileBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 2)
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QueueCard(item: QueueItem, onPreview: () -> Unit = {}) {
    val isPlaying = AudioPlayer.playingId == item.id
    Card(
        onClick = {
            if (item.isAudio) AudioPlayer.toggle(item.id, item.fileUrl)
            else if (!item.fileUrl.isNullOrBlank()) onPreview()
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail: real image for wallpapers, music tile for audio
            if (!item.isAudio && !item.fileUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.fileUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).background(Color(0xFF201A1C), RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    Modifier.size(72.dp).background(Color(0xFF201A1C), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = if (isPlaying) PrimaryPink else Violet,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title ?: item.name ?: item.id,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (item.category ?: "OTHER").uppercase(Locale.US),
                        color = ChipText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatBytes(item.size), color = TextMuted, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (item.tags ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3).forEach { tag ->
                        Box(Modifier.background(ChipBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(tag, color = ChipText, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            val isUploaded = item.status == "uploaded" || item.status == "done" || item.status == "published"
            Box(
                Modifier.background(if (isUploaded) PillGreenBg else PillBlueBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    (item.status ?: "queued").uppercase(Locale.US),
                    color = if (isUploaded) PillGreenText else PillBlueText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

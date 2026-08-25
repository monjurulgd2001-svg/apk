package com.zedge.automation.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zedge.automation.data.QueueItem
import com.zedge.automation.ui.AudioPlayer
import com.zedge.automation.ui.AudioProgressBar
import com.zedge.automation.ui.AudioProgressPoller
import com.zedge.automation.ui.GlassCard
import com.zedge.automation.ui.GlowDot
import com.zedge.automation.ui.SectionHeader
import com.zedge.automation.ui.StatusBadge
import com.zedge.automation.ui.WallpaperPreviewDialog
import com.zedge.automation.ui.theme.AccentGradient
import com.zedge.automation.ui.theme.ChipBg
import com.zedge.automation.ui.theme.ChipText
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.GlassPanel
import com.zedge.automation.ui.theme.GlassPanelStrong
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.SkyBlueLight
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.ui.theme.VioletLight
import com.zedge.automation.viewmodel.MainViewModel
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val sizes = listOf("B", "KB", "MB")
    val i = minOf((Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt(), 2)
    return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024.0, i.toDouble()), sizes[i])
}

/** Mission-control style glass dashboard fed with the user's live queue data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, onViewAll: () -> Unit = {}) {
    val items by vm.queueItems.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var previewItem by remember { mutableStateOf<QueueItem?>(null) }

    val total = items.size
    val queued = items.count { (it.status ?: "queued") == "queued" }
    val audios = items.count { it.isAudio }
    val wallpapers = total - audios

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { vm.refresh() }
    ) {
        // Single poller drives all audio progress bars on this screen
        AudioProgressPoller()
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // ── Hero: one large mission-control glass panel ──
                GlassCard(container = GlassPanelStrong, corner = 26) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "MISSION CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            GlowDot(MintGreen, 6)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "LIVE",
                                color = MintGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$total",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "assets in pipeline",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x2922D3EE))
                                    .border(1.dp, Color(0x5922D3EE), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    activeProject.uppercase(Locale.US),
                                    color = SkyBlueLight,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = GlassBorder)
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Metric("$queued", "Queued", SkyBlue)
                            Metric("$audios", "Audio", PrimaryPink)
                            Metric("$wallpapers", "Visual", MintGreen)
                            Metric(activeProject.uppercase(Locale.US), "Database", VioletLight)
                        }
                    }
                }
            }
            item {
                SectionHeader(
                    "Live Queue",
                    subtitle = "Latest 12 assets",
                    trailing = {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(ChipBg)
                                .border(1.dp, Color(0x59A855F7), RoundedCornerShape(18.dp))
                                .clickable { onViewAll() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "VIEW ALL",
                                color = ChipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                )
            }
            items(items.take(12), key = { it.id }) { item -> QueueCard(item, onPreview = { previewItem = item }) }
        }
    }

    previewItem?.let { p ->
        val url = p.fileUrl
        if (!url.isNullOrBlank()) {
            WallpaperPreviewDialog(url = url, title = p.title ?: p.name ?: "", onDismiss = { previewItem = null })
        }
    }
}

@Composable
private fun Metric(value: String, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = tint,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            label.uppercase(Locale.US),
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}

/** New queue row — glass strip card with a neon edge bar instead of the old fat cards. */
@Composable
private fun QueueCard(item: QueueItem, onPreview: () -> Unit = {}) {
    val isPlaying = AudioPlayer.playingId == item.id
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(GlassPanel)
            .border(1.dp, GlassBorder, shape)
            .clickable {
                if (item.isAudio) AudioPlayer.toggle(item.id, item.fileUrl)
                else if (!item.fileUrl.isNullOrBlank()) onPreview()
            }
    ) {
        // Neon identity strip: pink→violet for audio, cyan→mint for wallpapers
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    if (item.isAudio) Brush.verticalGradient(listOf(PrimaryPink, Violet))
                    else Brush.verticalGradient(listOf(SkyBlue, MintGreen))
                )
        )
        Column(Modifier.weight(1f).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isAudio && !item.fileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.fileUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPlaying) AccentGradient else Color(0x1AFFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) Color.White else VioletLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.title ?: item.name ?: item.id,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(6.dp))
                        StatusBadge(item.status ?: "queued")
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${(item.category ?: "OTHER").uppercase(Locale.US)} \u00b7 ${formatBytes(item.size)}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    val tagLine = (item.tags ?: "")
                        .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        .take(3).joinToString("  ") { "#$it" }
                    if (tagLine.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            tagLine,
                            color = ChipText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
            // Progress bar — appears below the row only while this audio is playing
            if (item.isAudio) {
                AudioProgressBar(itemId = item.id)
            }
        }
    }
}

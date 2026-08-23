package com.zedge.automation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.theme.AccentGradient
import com.zedge.automation.ui.theme.PrimaryGradient
import com.zedge.automation.ui.theme.SecondaryGradient
import com.zedge.automation.ui.theme.SuccessGradient
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val sizes = listOf("B", "KB", "MB")
    val i = minOf((Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt(), 2)
    return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024.0, i.toDouble()), sizes[i])
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    val items by vm.queueItems.collectAsState()
    val uploadState by vm.uploadState.collectAsState()
    val activeProject by vm.activeProject.collectAsState()

    val total = items.size
    val queued = items.count { (it.status ?: "queued") == "queued" }
    val uploaded = items.count { it.status == "uploaded" || it.status == "done" || it.status == "published" }
    val ringtones = items.count { it.isAudio }
    val wallpapers = total - ringtones

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Dashboard — $activeProject", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Live sync with the web dashboard (same Firebase RTDB)", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total", total.toString(), PrimaryGradient, Modifier.weight(1f))
                StatCard("Queued", queued.toString(), SecondaryGradient, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Uploaded", uploaded.toString(), SuccessGradient, Modifier.weight(1f))
                StatCard("\uD83C\uDFB5 $ringtones · \uD83D\uDDBC $wallpapers", "Media", AccentGradient, Modifier.weight(1f))
            }
        }
        if (uploadState.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Upload State", fontWeight = FontWeight.SemiBold)
                        uploadState.entries.take(8).forEach { (k, v) ->
                            Text("$k: $v", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }
        item {
            Text("Recent uploads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(items.take(15)) { item ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (item.isAudio) Icons.Filled.MusicNote else Icons.Filled.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (item.isAudio) AccentGradient else SecondaryGradient, CircleShape)
                            .padding(8.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title ?: item.name ?: item.id, maxLines = 1, fontWeight = FontWeight.Medium)
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
                        }
                        Text(
                            "${item.status ?: "queued"} · ${formatBytes(item.size)} · ${if (item.createdAt > 0) sdf.format(Date(item.createdAt)) else ""}",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, gradient: Brush, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.background(gradient).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

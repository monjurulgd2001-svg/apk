package com.zedge.automation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel

/**
 * Distribute Image — same round-robin pipe as the web dashboard: images are
 * distributed evenly across the Zedge accounts (zedge1, zedge2, ...) in order.
 */
@Composable
fun DistributeScreen(vm: MainViewModel) {
    val progress by vm.progress.collectAsState()
    val items by vm.queueItems.collectAsState()

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.distributeImages(uris)
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("\uD83D\uDDBC\uFE0F Distribute Image", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Load wallpaper images into your round-robin automation pipe. Media distributes evenly across Zedge accounts in order.",
                color = TextMuted, style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { pickImages.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) { Text("Select Images", fontWeight = FontWeight.SemiBold) }

                    if (progress.total > 0 && progress.current < progress.total) {
                        LinearProgressIndicator(
                            progress = { progress.current.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth(), color = SkyBlue
                        )
                    }
                    Text(
                        progress.message.ifBlank { "Waiting for image selection..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress.error) MaterialTheme.colorScheme.error else TextMuted
                    )
                }
            }
        }
        item {
            Text("Recently distributed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(items.filter { it.distributedTo != null }.take(20)) { item ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.title ?: item.name ?: item.id, maxLines = 1, fontWeight = FontWeight.Medium)
                    Text(
                        "\u2192 ${item.distributedTo} · ${item.category ?: "OTHER"} · ${formatBytes(item.size)}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                }
            }
        }
    }
}

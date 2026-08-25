package com.zedge.automation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.GlassCard
import com.zedge.automation.ui.GlowDot
import com.zedge.automation.ui.IconTile
import com.zedge.automation.ui.NeonButton
import com.zedge.automation.ui.SectionHeader
import com.zedge.automation.ui.theme.BlueSoft
import com.zedge.automation.ui.theme.GreenSoft
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.SkyBlueLight
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
    // v3.5: compute once per queue update instead of on every recomposition.
    val distributed = remember(items) { items.filter { it.distributedTo != null }.take(20) }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.distributeImages(uris)
    }

    val cyanSweep = remember { Brush.horizontalGradient(listOf(SkyBlue, Violet)) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionHeader(
                "Distribute",
                subtitle = "Round-robin pipe — media spreads evenly across Zedge accounts in order"
            )
        }
        item {
            GlassCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Filled.Share, SkyBlue, BlueSoft, 46, 14)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Image Pipeline", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                "zedge1 \u2192 zedge2 \u2192 \u2026 in strict order",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    NeonButton(
                        "Select Images",
                        modifier = Modifier.fillMaxWidth(),
                        gradient = cyanSweep,
                        icon = Icons.Filled.Image
                    ) { pickImages.launch("image/*") }

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
        item { SectionHeader("Recently Distributed") }
        items(distributed, key = { it.id }) { item ->
            GlassCard(corner = 16) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Filled.Image, MintGreen, GreenSoft, 38, 12)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title ?: item.name ?: item.id,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            "${item.category ?: "OTHER"} \u00b7 ${formatBytes(item.size)}",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlowDot(MintGreen, 5)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            item.distributedTo ?: "",
                            color = SkyBlueLight,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

package com.zedge.automation.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PastelOrange
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel

@Composable
fun AiStudioScreen(vm: MainViewModel) {
    val bulkStatuses by vm.bulkStatuses.collectAsState()

    var duration by remember { mutableFloatStateOf(30f) }
    var gainPercent by remember { mutableFloatStateOf(200f) }
    var silenceThreshold by remember { mutableFloatStateOf(0.02f) }
    var padMs by remember { mutableFloatStateOf(100f) }
    var autoTrimBoost by remember { mutableStateOf(false) }
    var bulkPrompts by remember { mutableStateOf("") }

    val promptCount = bulkPrompts.lines().map { it.trim() }.filter { it.isNotEmpty() }.size
    val doneCount = bulkStatuses.count { it.status.startsWith("Done") }
    val failCount = bulkStatuses.count { it.status.startsWith("Failed") }
    val runningCount = bulkStatuses.count { it.status == "Composing" || it.status == "Processing audio..." }
    val isRunning = bulkStatuses.any { it.status == "Composing" || it.status == "Processing audio..." }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──
        item {
            Column {
                Text(
                    "Bulk AI Generation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "One prompt per line — each one is generated, processed and queued automatically.",
                    color = TextMuted, style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ── Main Card ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prompt input
                    OutlinedTextField(
                        bulkPrompts, { bulkPrompts = it },
                        label = { Text("Enter prompts...") },
                        placeholder = { Text("Piano sad emotional melody\nEnergetic electronic dance 128 bpm\nChill lo-fi hip hop beat") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f)
                        )
                    )

                    // Prompt count badge
                    if (promptCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Violet)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$promptCount prompt${if (promptCount != 1) "s" else ""} ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = Violet, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Divider
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // Settings header
                    Text(
                        "Generation Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )

                    // Settings grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingInput("Duration", "${duration.toInt()}s", Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> duration = v.coerceIn(5f, 180f) }
                        }
                        SettingInput("Gain %", "${gainPercent.toInt()}%", Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> gainPercent = v.coerceIn(10f, 500f) }
                        }
                        SettingInput("Silence", silenceThreshold.toString(), Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> silenceThreshold = v.coerceIn(0.001f, 0.5f) }
                        }
                        SettingInput("Pad ms", "${padMs.toInt()}", Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> padMs = v.coerceIn(0f, 1000f) }
                        }
                    }

                    // Auto trim toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = autoTrimBoost,
                            onCheckedChange = { autoTrimBoost = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPink)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Auto trim + boost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Apply gain, silence trim & padding", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val prompts = bulkPrompts.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                if (prompts.isNotEmpty()) vm.startBulkGeneration(
                                    prompts, duration.toInt(), gainPercent, silenceThreshold, padMs.toInt(), autoTrimBoost
                                )
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Violet),
                            enabled = promptCount > 0 && !isRunning
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate All", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { vm.stopBulkGeneration() },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = isRunning
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(18.dp))
                        }
                    }

                    // Progress bar when running
                    if (isRunning) {
                        val total = bulkStatuses.size
                        val progress = if (total > 0) (doneCount + failCount).toFloat() / total else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Violet,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // ── Status summary cards ──
        if (bulkStatuses.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusPill("Done", doneCount, MintGreen, Icons.Filled.CheckCircle, Modifier.weight(1f))
                    StatusPill("Running", runningCount, PastelOrange, Icons.Filled.HourglassBottom, Modifier.weight(1f))
                    StatusPill("Failed", failCount, MaterialTheme.colorScheme.error, Icons.Filled.Error, Modifier.weight(1f))
                }
            }
        }

        // ── Status list ──
        items(bulkStatuses) { s ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status icon
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    s.status.startsWith("Done") -> MintGreen.copy(alpha = 0.15f)
                                    s.status.startsWith("Failed") -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    s.status == "Composing" -> PastelOrange.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                s.status.startsWith("Done") -> Icons.Filled.CheckCircle
                                s.status.startsWith("Failed") -> Icons.Filled.Error
                                s.status == "Composing" -> Icons.Filled.MusicNote
                                else -> Icons.Filled.HourglassBottom
                            },
                            contentDescription = null,
                            tint = when {
                                s.status.startsWith("Done") -> MintGreen
                                s.status.startsWith("Failed") -> MaterialTheme.colorScheme.error
                                s.status == "Composing" -> PastelOrange
                                else -> TextMuted
                            },
                            Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.prompt, maxLines = 1, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.status, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                        color = when {
                            s.status.startsWith("Done") -> MintGreen
                            s.status.startsWith("Failed") -> MaterialTheme.colorScheme.error
                            s.status == "Composing" -> PastelOrange
                            else -> TextMuted
                        }
                    )
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun StatusPill(label: String, count: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("$count", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(4.dp))
            Text(label, color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

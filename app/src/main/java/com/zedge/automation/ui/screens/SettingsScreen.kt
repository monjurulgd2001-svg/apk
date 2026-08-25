package com.zedge.automation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.GlassPanel
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.ThemeState
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel

/**
 * Settings — same keys as the web dashboard's localStorage:
 * geminiApiKeys (one per line, auto-rotate), geminiModel, stableAudioToken.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onStableAudioLogin: () -> Unit = {}) {
    val settings = vm.settings
    var keysText by remember { mutableStateOf(settings.geminiApiKeys.joinToString("\n")) }
    var model by remember { mutableStateOf(settings.geminiModel) }
    var token by remember { mutableStateOf(settings.stableAudioToken) }
    var savedMsg by remember { mutableStateOf("") }

    val models = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro")

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("\u2699\uFE0F Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Keyগুলো শুধু এই ডিভাইসে সেভ থাকে — ওয়েব ড্যাশবোর্ডের localStorage-এর মতোই", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = GlassPanel), border = BorderStroke(1.dp, GlassBorder)) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Appearance", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (ThemeState.isDark) "Dark mode active" else "Light mode active",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted
                        )
                    }
                    Button(
                        onClick = { ThemeState.isDark = !ThemeState.isDark },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ThemeState.isDark) Violet else PrimaryPink
                        )
                    ) {
                        Text(if (ThemeState.isDark) "\u263E Night" else "\u2600 Day", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = GlassPanel), border = BorderStroke(1.dp, GlassBorder)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("\uD83E\uDD16 Gemini AI Metadata", fontWeight = FontWeight.SemiBold)
                        AssistChip(onClick = {}, label = { Text("${settings.geminiApiKeys.size} key(s)") })
                    }
                    Text(
                        "aistudio.google.com → Get API key। এক লাইনে একটা key। একাধিক key দিলে অটো-রোটেট হবে।",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                    OutlinedTextField(
                        keysText, { keysText = it },
                        label = { Text("Gemini API Keys (one per line)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3
                    )
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = model, onValueChange = {}, readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            models.forEach { m ->
                                DropdownMenuItem(text = { Text(m) }, onClick = { model = m; expanded = false })
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    settings.geminiApiKeys = keysText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    settings.geminiModel = model
                    settings.stableAudioToken = token.trim()
                    savedMsg = "\u2705 Settings saved!"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) { Text("Save Settings", fontWeight = FontWeight.SemiBold) }
            if (savedMsg.isNotBlank()) {
                Text(savedMsg, color = MintGreen, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

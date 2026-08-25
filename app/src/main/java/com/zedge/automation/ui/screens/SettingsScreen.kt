package com.zedge.automation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.GlassCard
import com.zedge.automation.ui.IconTile
import com.zedge.automation.ui.NeonButton
import com.zedge.automation.ui.SectionHeader
import com.zedge.automation.ui.theme.ChipBg
import com.zedge.automation.ui.theme.ChipText
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.ThemeState
import com.zedge.automation.ui.theme.VioletLight
import com.zedge.automation.ui.theme.VioletSoft
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
            SectionHeader(
                "Settings",
                subtitle = "Keyগুলো শুধু এই ডিভাইসে সেভ থাকে — ওয়েব ড্যাশবোর্ডের localStorage-এর মতোই"
            )
        }
        item {
            GlassCard {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(Icons.Filled.DarkMode, VioletLight, VioletSoft, 42, 13)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Appearance", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (ThemeState.isDark) "Dark mode active" else "Light mode active",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    NeonButton(
                        if (ThemeState.isDark) "\u263e Night" else "\u2600 Day"
                    ) { ThemeState.isDark = !ThemeState.isDark }
                }
            }
        }
        item {
            GlassCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconTile(Icons.Filled.AutoFixHigh, VioletLight, VioletSoft, 42, 13)
                        Column(Modifier.weight(1f)) {
                            Text("Gemini AI Metadata", fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Auto title \u00b7 tags \u00b7 category", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(ChipBg)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "${settings.geminiApiKeys.size} key(s)",
                                color = ChipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
            NeonButton("Save Settings", modifier = Modifier.fillMaxWidth()) {
                settings.geminiApiKeys = keysText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                settings.geminiModel = model
                settings.stableAudioToken = token.trim()
                savedMsg = "\u2705 Settings saved!"
            }
            if (savedMsg.isNotBlank()) {
                Text(savedMsg, color = MintGreen, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

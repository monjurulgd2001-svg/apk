package com.zedge.automation.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PastelOrange
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AiStudioScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    val bulkStatuses by vm.bulkStatuses.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var duration by remember { mutableFloatStateOf(30f) }
    var gainPercent by remember { mutableFloatStateOf(200f) }
    var silenceThreshold by remember { mutableFloatStateOf(0.02f) }
    var padMs by remember { mutableFloatStateOf(100f) }
    var autoTrimBoost by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var generatedAudio by remember { mutableStateOf<ByteArray?>(null) }
    var generatedMime by remember { mutableStateOf("audio/mpeg") }
    var bulkPrompts by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun playAudio(bytes: ByteArray, mime: String, context: android.content.Context) {
        mediaPlayer?.release()
        val ext = if (mime.contains("wav")) ".wav" else ".mp3"
        val tmp = File.createTempFile("ai_ringtone", ext, context.cacheDir)
        tmp.writeBytes(bytes)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(tmp.absolutePath)
            prepare(); start()
        }
    }

    val context = LocalContext.current

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("AI Ringtone Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Stable Audio music generation + Gemini auto-metadata", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }

        // ── Single Generate ──
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        prompt, { prompt = it },
                        label = { Text("Prompt (e.g. Piano sad emotional melody)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                    OutlinedButton(
                        enabled = !busy && vm.settings.hasGeminiKeys(),
                        onClick = {
                            scope.launch {
                                busy = true; isError = false
                                try {
                                    status = "Gemini writing random prompt..."
                                    prompt = vm.gemini.gemini("Write ONE short creative text-to-music prompt for a mobile ringtone (instruments + mood + genre). Return ONLY the prompt.")
                                    status = ""
                                } catch (e: Exception) { status = "Prompt failed: ${e.message}"; isError = true }
                                busy = false
                            }
                        }
                    ) { Text("Random prompt (Gemini)") }

                    Text("Duration: ${duration.toInt()}s", style = MaterialTheme.typography.bodySmall)
                    Slider(duration, { duration = it }, valueRange = 5f..180f,
                        colors = SliderDefaults.colors(thumbColor = Violet, activeTrackColor = Violet))

                    Button(
                        enabled = !busy && prompt.isNotBlank(),
                        onClick = {
                            scope.launch {
                                busy = true; isError = false; generatedAudio = null
                                try {
                                    if (!vm.stableAudio.isTokenValid()) {
                                        status = "Stable Audio token not set. Go to Settings → Auto-Create Account."
                                        isError = true; busy = false; return@launch
                                    }
                                    var bytes = vm.generateRingtone(prompt.trim(), duration.toInt()) { status = it }
                                    var mime = "audio/mpeg"
                                    if (autoTrimBoost) {
                                        status = "Applying gain & trim..."
                                        val result = processAudio(bytes, gainPercent / 100f, silenceThreshold, padMs.toInt())
                                        bytes = result.data
                                        mime = result.mimeType
                                    }
                                    generatedAudio = bytes
                                    generatedMime = mime
                                    status = "Generated! Play or add to queue."
                                } catch (e: Exception) { status = "${e.message}"; isError = true }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) { Text(if (busy) "Working..." else "Generate Audio", fontWeight = FontWeight.SemiBold) }

                    if (generatedAudio != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { playAudio(generatedAudio!!, generatedMime, context) }, modifier = Modifier.weight(1f)) { Text("Play") }
                            Button(
                                onClick = {
                                    status = "Uploading to queue..."
                                    vm.addGeneratedToQueue(generatedAudio!!, prompt.trim(), duration.toDouble(), generatedMime) { err ->
                                        status = if (err == null) "Added to Upload Queue!" else "$err"
                                        isError = err != null
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                            ) { Text("Add to Queue") }
                        }
                    }
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.error else TextMuted)
                    }
                }
            }
        }

        // ── Bulk AI Generation ──
        item {
            Text("Bulk AI Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("One prompt per line \u2014 each one is generated, processed and queued automatically.",
                color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        bulkPrompts, { bulkPrompts = it },
                        label = { Text("One prompt per line") },
                        modifier = Modifier.fillMaxWidth(), minLines = 4
                    )

                    val promptCount = bulkPrompts.lines().map { it.trim() }.filter { it.isNotEmpty() }.size
                    Text("$promptCount prompts", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                    // Settings row — Duration, Gain, Silence, Pad
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            OutlinedTextField(
                                value = duration.toInt().toString(),
                                onValueChange = { it.toFloatOrNull()?.let { v -> duration = v.coerceIn(5f, 180f) } },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gain %", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            OutlinedTextField(
                                value = gainPercent.toInt().toString(),
                                onValueChange = { it.toFloatOrNull()?.let { v -> gainPercent = v.coerceIn(10f, 500f) } },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Silence", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            OutlinedTextField(
                                value = silenceThreshold.toString(),
                                onValueChange = { it.toFloatOrNull()?.let { v -> silenceThreshold = v.coerceIn(0.001f, 0.5f) } },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pad ms", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            OutlinedTextField(
                                value = padMs.toInt().toString(),
                                onValueChange = { it.toFloatOrNull()?.let { v -> padMs = v.coerceIn(0f, 1000f) } },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                singleLine = true
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = autoTrimBoost,
                            onCheckedChange = { autoTrimBoost = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPink)
                        )
                        Text("Auto trim + boost", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val prompts = bulkPrompts.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                if (prompts.isNotEmpty()) vm.startBulkGeneration(
                                    prompts, duration.toInt(), gainPercent, silenceThreshold, padMs.toInt(), autoTrimBoost
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
                        ) { Text("Generate All") }
                        OutlinedButton(onClick = { vm.stopBulkGeneration() }, modifier = Modifier.weight(1f)) { Text("Stop") }
                    }
                }
            }
        }
        items(bulkStatuses) { s ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.prompt, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall)
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
    }
}

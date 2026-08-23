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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PastelOrange
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
    var duration by remember { mutableStateOf(30f) }
    var status by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var generatedAudio by remember { mutableStateOf<ByteArray?>(null) }
    var bulkPrompts by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun playAudio(bytes: ByteArray, context: android.content.Context) {
        mediaPlayer?.release()
        val tmp = File.createTempFile("ai_ringtone", ".mp3", context.cacheDir)
        tmp.writeBytes(bytes)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(tmp.absolutePath)
            prepare(); start()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("\uD83E\uDE84 AI Ringtone Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Stable Audio দিয়ে মিউজিক জেনারেট + Gemini মেটাডাটা", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        prompt, { prompt = it },
                        label = { Text("Prompt (e.g. Piano sad emotional melody ambient pad)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                    OutlinedButton(
                        enabled = !busy && vm.settings.hasGeminiKeys(),
                        onClick = {
                            scope.launch {
                                busy = true; isError = false
                                try {
                                    status = "\uD83E\uDE84 Gemini is writing a random prompt..."
                                    prompt = vm.gemini.gemini("Write ONE short creative text-to-music prompt for a mobile ringtone (instruments + mood + genre). Return ONLY the prompt.")
                                    status = ""
                                } catch (e: Exception) { status = "Prompt failed: ${e.message}"; isError = true }
                                busy = false
                            }
                        }
                    ) { Text("\uD83E\uDE84 Random prompt (Gemini)") }
                    Text("Duration: ${duration.toInt()}s", style = MaterialTheme.typography.bodySmall)
                    Slider(duration, { duration = it }, valueRange = 5f..180f)
                    Button(
                        enabled = !busy && prompt.isNotBlank(),
                        onClick = {
                            scope.launch {
                                busy = true; isError = false; generatedAudio = null
                                try {
                                    val bytes = vm.generateRingtone(prompt.trim(), duration.toInt()) { status = it }
                                    generatedAudio = bytes
                                    status = "\u2705 Generated! Play it or add to queue."
                                } catch (e: Exception) { status = "\u26A0\uFE0F ${e.message}"; isError = true }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) { Text(if (busy) "Working..." else "Generate Audio", fontWeight = FontWeight.SemiBold) }

                    if (generatedAudio != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { playAudio(generatedAudio!!, context) }, modifier = Modifier.weight(1f)) { Text("\u25B6 Play") }
                            Button(
                                onClick = {
                                    status = "Uploading to queue..."
                                    vm.addGeneratedToQueue(generatedAudio!!, prompt.trim(), duration.toDouble()) { err ->
                                        status = if (err == null) "\u2705 Added to Upload Queue!" else "\u26A0\uFE0F $err"
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
        item {
            Text("Bulk AI Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        bulkPrompts, { bulkPrompts = it },
                        label = { Text("One prompt per line") },
                        modifier = Modifier.fillMaxWidth(), minLines = 4
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val prompts = bulkPrompts.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                if (prompts.isNotEmpty()) vm.startBulkGeneration(prompts, duration.toInt())
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
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.prompt, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    Text(
                        s.status, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                        color = when (s.status) {
                            "Done" -> MintGreen
                            "Failed" -> MaterialTheme.colorScheme.error
                            "Composing" -> PastelOrange
                            else -> TextMuted
                        }
                    )
                }
            }
        }
    }
}

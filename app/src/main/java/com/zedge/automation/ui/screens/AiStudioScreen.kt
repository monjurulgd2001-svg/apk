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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.zedge.automation.data.StableAudioAuth
import androidx.compose.foundation.BorderStroke
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.GlassPanel
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private val CardBg = Color(0x26A855F7)      // frosted neon-purple glass
private val CardBg2 = Color(0x2422D3EE)     // frosted neon-cyan glass
private val AccentPink = Color(0xFFC084FC)  // neon purple accent

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(vm: MainViewModel) {
    val bulkStatuses by vm.bulkStatuses.collectAsState()

    val scope = rememberCoroutineScope()
    val auth = remember { StableAudioAuth(vm.settings) }
    var hasToken by remember { mutableStateOf(vm.settings.stableAudioToken.isNotBlank()) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var webViewRequested by remember { mutableStateOf(false) }
    var authStatus by remember { mutableStateOf("") }
    var authBusy by remember { mutableStateOf(false) }
    var authSuccess by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf(false) }

    var duration by remember { mutableFloatStateOf(30f) }

    var gainPercent by remember { mutableFloatStateOf(200f) }
    var silenceThreshold by remember { mutableFloatStateOf(0.02f) }
    var padMs by remember { mutableFloatStateOf(100f) }
    var autoTrimBoost by remember { mutableStateOf(false) }
    var bulkPrompts by remember { mutableStateOf("") }

    val promptCount by remember { derivedStateOf { bulkPrompts.lines().count { it.trim().isNotEmpty() } } }
    val doneCount    by remember { derivedStateOf { bulkStatuses.count { it.status.startsWith("Done") } } }
    val failCount    by remember { derivedStateOf { bulkStatuses.count { it.status.startsWith("Failed") } } }
    val runningCount by remember { derivedStateOf { bulkStatuses.count { it.status == "Composing" || it.status == "Processing audio..." || it.status == "Generating metadata..." } } }
    val isRunning    by remember { derivedStateOf { bulkStatuses.any  { it.status == "Composing" || it.status == "Processing audio..." || it.status == "Generating metadata..." } } }

    Box(Modifier.fillMaxSize()) {

    LazyColumn(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Stable Audio Card ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, GlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(34.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Stable Audio", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                            Text("Music Generation Engine", color = TextMuted, fontSize = 10.sp)
                        }
                        Text(
                            if (hasToken) "Active" else "No Token",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasToken) Color(0xFF5EF0C0) else TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (authStatus.isNotBlank()) {
                        Text(
                            authStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                authBusy = true; authError = false; authSuccess = false
                                if (!webViewRequested) {
                                    webViewRequested = true
                                    authStatus = "Loading WebView..."
                                    var waited = 0
                                    while (webViewRef.value == null && waited < 8000) {
                                        kotlinx.coroutines.delay(200)
                                        waited += 200
                                    }
                                }
                                val wv = webViewRef.value
                                if (wv == null) {
                                    authStatus = "WebView not ready."
                                    authBusy = false; return@launch
                                }
                                authStatus = "Starting automation..."
                                val result = auth.performAutoAccountCreation(wv) { msg -> authStatus = msg }
                                authBusy = false
                                if (result.success) {
                                    authSuccess = true; hasToken = true
                                    authStatus = "Token synced!"
                                } else {
                                    authError = true
                                    authStatus = result.error ?: "Failed"
                                }
                            }
                        },
                        enabled = !authBusy,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPink,
                            disabledContainerColor = Color.White.copy(alpha = 0.06f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (authBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (authBusy) "Creating account..." else "Auto-Create Account & Sync Token",
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Header ──
        item {
            Column {
                Text(
                    "Bulk AI Generation",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "One prompt per line, each one generated and queued automatically.",
                    color = TextMuted, style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ── Main Card ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg2),
                border = BorderStroke(1.dp, GlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        bulkPrompts, { bulkPrompts = it },
                        label = { Text("Enter prompts...") },
                        placeholder = {
                            Text("Piano sad emotional melody\nEnergetic electronic dance\nChill lo-fi hip hop beat")
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPink.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.04f),
                            cursorColor = AccentPink,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (promptCount > 0) {
                        Text(
                            "$promptCount prompt${if (promptCount != 1) "s" else ""} ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

                    Text("Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextMuted)

                    // Duration slider
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("${duration.toInt()}s", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value = duration,
                            onValueChange = { duration = it },
                            valueRange = 2f..180f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            thumb = {
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(Color.White, CircleShape)
                                )
                            },
                            track = { sliderState ->
                                val fraction = (sliderState.value - 2f) / (180f - 2f)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(fraction)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFA855F7), Color(0xFF22D3EE), Color(0xFF67E8F9))
                                                )
                                            )
                                    )
                                }
                            }
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SimpleSettingInput("Gain %", "${gainPercent.toInt()}", Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> gainPercent = v.coerceIn(10f, 500f) }
                        }
                        SimpleSettingInput("Silence", silenceThreshold.toString(), Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> silenceThreshold = v.coerceIn(0.001f, 0.5f) }
                        }
                        SimpleSettingInput("Pad ms", "${padMs.toInt()}", Modifier.weight(1f)) {
                            it.toFloatOrNull()?.let { v -> padMs = v.coerceIn(0f, 1000f) }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoTrimBoost,
                            onCheckedChange = { autoTrimBoost = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentPink)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Auto trim + boost", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text("Apply gain, silence trim & padding", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val prompts = bulkPrompts.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                if (prompts.isNotEmpty()) vm.startBulkGeneration(
                                    prompts, duration.toInt(), gainPercent, silenceThreshold, padMs.toInt(), autoTrimBoost
                                )
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPink,
                                disabledContainerColor = Color.White.copy(alpha = 0.06f)
                            ),
                            enabled = promptCount > 0 && !isRunning
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Generate All", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { vm.stopBulkGeneration() },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                            enabled = isRunning
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(14.dp))
                        }
                    }

                    if (isRunning) {
                        val total = bulkStatuses.size
                        val progress = if (total > 0) (doneCount + failCount).toFloat() / total else 0f
                        Column {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AccentPink,
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${doneCount + failCount}/$total", fontSize = 10.sp, color = TextMuted)
                                Text("${(progress * 100).toInt()}%", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // ── Status Summary ──
        if (bulkStatuses.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniPill("$doneCount Done", Modifier.weight(1f))
                    MiniPill("$runningCount Running", Modifier.weight(1f))
                    MiniPill("$failCount Failed", Modifier.weight(1f))
                }
            }
        }

        // ── Status List ──
        itemsIndexed(bulkStatuses, key = { i, _ -> i }) { _, s ->
            val isDone = s.status.startsWith("Done")
            val isFailed = s.status.startsWith("Failed")

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = GlassPanel),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when {
                            isDone -> Icons.Filled.CheckCircle
                            isFailed -> Icons.Filled.Error
                            else -> Icons.Filled.MusicNote
                        },
                        contentDescription = null,
                        tint = when {
                            isDone -> Color(0xFF5EF0C0)
                            isFailed -> Color(0xFFFBBF24)
                            else -> TextMuted
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(s.prompt, maxLines = 1, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(s.status, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    // Hidden WebView
    if (webViewRequested) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    alpha = 0f
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.userAgentString = settings.userAgentString.replace("wv", "")
                    val wv = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(wv, true)
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            settings.domStorageEnabled = true
                        }
                    }
                    webChromeClient = WebChromeClient()
                    webViewRef.value = this
                    loadUrl(StableAudioAuth.STABLE_AUDIO_URL)
                }
            },
            modifier = Modifier.fillMaxWidth().height(1.dp)
        )
    }

    }
}

@Composable
private fun SimpleSettingInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPink.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = Color.Black.copy(alpha = 0.06f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.03f),
                cursorColor = AccentPink,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
private fun MiniPill(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        fontWeight = FontWeight.Medium
    )
}

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
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private val CardBg = Color(0xFF2A1F3D)
private val CardBg2 = Color(0xFF1E2A3A)
private val AccentPurple = Color(0xFF9B6DFF)
private val AccentCyan = Color(0xFF56CCF2)
private val AccentGold = Color(0xFFF2C94C)
private val AccentCoral = Color(0xFFF2994A)
private val AccentMint = Color(0xFF6FCF97)

@SuppressLint("SetJavaScriptEnabled")
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
        // ═══════════════════════════════════════════
        // ── Stable Audio Card ──
        // ═══════════════════════════════════════════
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(AccentPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Stable Audio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Music Generation Engine", color = TextMuted, fontSize = 11.sp)
                        }
                        Box(
                            Modifier
                                .background(
                                    if (hasToken) AccentMint.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (hasToken) "\u2713 Active" else "No Token",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasToken) AccentMint else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (authStatus.isNotBlank()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (authBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentCyan, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    authStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        authError -> AccentCoral
                                        authSuccess -> AccentMint
                                        else -> TextMuted
                                    }
                                )
                            }
                        }
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
                                    authStatus = "\u2713 Token synced!"
                                } else {
                                    authError = true
                                    authStatus = result.error ?: "Failed"
                                }
                            }
                        },
                        enabled = !authBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (authBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (authBusy) "Creating account..." else "Auto-Create Account & Sync Token",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════
        // ── Header ──
        // ═══════════════════════════════════════════
        item {
            Column {
                Text(
                    "\uD83C\uDFB5 Bulk AI Generation",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryPink)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "One prompt per line — each one is generated, processed and queued automatically.",
                    color = TextMuted, style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ═══════════════════════════════════════════
        // ── Main Card ──
        // ═══════════════════════════════════════════
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg2),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Prompt input
                    OutlinedTextField(
                        bulkPrompts, { bulkPrompts = it },
                        label = { Text("Enter prompts...") },
                        placeholder = {
                            Text("Piano sad emotional melody\nEnergetic electronic dance\nChill lo-fi hip hop beat")
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.06f),
                            cursorColor = PrimaryPink,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Prompt count
                    if (promptCount > 0) {
                        Text(
                            "$promptCount prompt${if (promptCount != 1) "s" else ""} ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentMint, fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Divider
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // Settings header
                    Text("Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextMuted)

                    // Duration
                    SimpleSettingInput("Duration (s)", "${duration.toInt()}", Modifier.fillMaxWidth()) {
                        it.toFloatOrNull()?.let { v -> duration = v.coerceIn(1f, 180f) }
                    }

                    // 3 compact fields
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

                    // Auto trim toggle
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoTrimBoost,
                            onCheckedChange = { autoTrimBoost = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPink)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Auto trim + boost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
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
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPink,
                                disabledContainerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            enabled = promptCount > 0 && !isRunning
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(5.dp))
                                Text("Generate All", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { vm.stopBulkGeneration() },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCoral),
                            enabled = isRunning
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(16.dp))
                        }
                    }

                    // Progress bar
                    if (isRunning) {
                        val total = bulkStatuses.size
                        val progress = if (total > 0) (doneCount + failCount).toFloat() / total else 0f
                        Column {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = PrimaryPink,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${doneCount + failCount}/$total", fontSize = 10.sp, color = TextMuted)
                                Text("${(progress * 100).toInt()}%", fontSize = 10.sp, color = PrimaryPink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════
        // ── Status Summary ──
        // ═══════════════════════════════════════════
        if (bulkStatuses.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("Done", doneCount, AccentMint, Icons.Filled.CheckCircle, Modifier.weight(1f))
                    StatusPill("Running", runningCount, AccentGold, Icons.Filled.HourglassBottom, Modifier.weight(1f))
                    StatusPill("Failed", failCount, AccentCoral, Icons.Filled.Error, Modifier.weight(1f))
                }
            }
        }

        // ═══════════════════════════════════════════
        // ── Status List ──
        // ═══════════════════════════════════════════
        itemsIndexed(bulkStatuses, key = { i, _ -> i }) { _, s ->
            val isWorking = s.status == "Composing" || s.status == "Processing audio..." || s.status == "Generating metadata..."
            val isDone = s.status.startsWith("Done")
            val isFailed = s.status.startsWith("Failed")

            val statusColor = when {
                isDone -> AccentMint
                isFailed -> AccentCoral
                isWorking -> AccentGold
                else -> TextMuted
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.04f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isDone -> Icons.Filled.CheckCircle
                                isFailed -> Icons.Filled.Error
                                isWorking -> Icons.Filled.MusicNote
                                else -> Icons.Filled.HourglassBottom
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.prompt, maxLines = 1, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.status, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
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

    } // end Box
}

// ── Simple Setting Input ──
@Composable
private fun SimpleSettingInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.Black.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.04f),
                cursorColor = PrimaryPink,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

// ── Status Pill ──
@Composable
private fun StatusPill(label: String, count: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text("$count", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
            Text(label, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

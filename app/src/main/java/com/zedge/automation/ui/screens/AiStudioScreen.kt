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
import androidx.compose.runtime.derivedStateOf
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
import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidView
import com.zedge.automation.data.StableAudioAuth
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PastelOrange
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AiStudioScreen(vm: MainViewModel) {
    val bulkStatuses by vm.bulkStatuses.collectAsState()

    // ── Stable Audio auth state ──
    val scope = rememberCoroutineScope()
    val auth = remember { StableAudioAuth(vm.settings) }
    // Read token once into state — avoids repeated SharedPreferences I/O on every recomposition
    var hasToken by remember { mutableStateOf(vm.settings.stableAudioToken.isNotBlank()) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    // Track whether the WebView has been triggered (lazy-load)
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

    // derivedStateOf → only recalculates when the underlying state actually changes
    val promptCount by remember { derivedStateOf { bulkPrompts.lines().count { it.trim().isNotEmpty() } } }
    val doneCount    by remember { derivedStateOf { bulkStatuses.count { it.status.startsWith("Done") } } }
    val failCount    by remember { derivedStateOf { bulkStatuses.count { it.status.startsWith("Failed") } } }
    val runningCount by remember { derivedStateOf { bulkStatuses.count { it.status == "Composing" || it.status == "Processing audio..." || it.status == "Generating metadata..." } } }
    val isRunning    by remember { derivedStateOf { bulkStatuses.any  { it.status == "Composing" || it.status == "Processing audio..." || it.status == "Generating metadata..." } } }

    Box(Modifier.fillMaxSize()) {

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Stable Audio Token (Auto-Create) ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "\uD83C\uDFB5 Stable Audio Account",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Box(
                            Modifier
                                .background(
                                    if (hasToken) MintGreen.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.07f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (hasToken) "\u2713 Active" else "No Token",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasToken) MintGreen else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (authStatus.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (authBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Violet, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                authStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    authError -> MaterialTheme.colorScheme.error
                                    authSuccess -> MintGreen
                                    else -> TextMuted
                                }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                authBusy = true; authError = false; authSuccess = false
                                // Trigger WebView lazy-load on first press
                                if (!webViewRequested) {
                                    webViewRequested = true
                                    authStatus = "Loading WebView, please wait..."
                                    // Give the WebView factory time to create & attach
                                    var waited = 0
                                    while (webViewRef.value == null && waited < 8000) {
                                        kotlinx.coroutines.delay(200)
                                        waited += 200
                                    }
                                }
                                val wv = webViewRef.value
                                if (wv == null) {
                                    authStatus = "WebView not ready, try again."
                                    authBusy = false; return@launch
                                }
                                authStatus = "Starting automation..."
                                val result = auth.performAutoAccountCreation(wv) { msg -> authStatus = msg }
                                authBusy = false
                                if (result.success) {
                                    authSuccess = true
                                    hasToken = true
                                    authStatus = "\u2713 Token synced! Ready for generation."
                                } else {
                                    authError = true
                                    authStatus = result.error ?: "Failed"
                                }
                            }
                        },
                        enabled = !authBusy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (authBusy) "Creating account..." else "Auto-Create Account & Sync Token",
                            fontWeight = FontWeight.SemiBold
                        )
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

                    // Settings grid — Duration alone (full width) + 3 compact fields in one row
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Duration — full width, prominent
                        SettingInput("Duration (s) · 1-180", "${duration.toInt()}", Modifier.fillMaxWidth()) {
                            it.toFloatOrNull()?.let { v -> duration = v.coerceIn(1f, 180f) }
                        }
                        // Gain, Silence, Pad — compact 3-column row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactSettingInput("Gain %", "${gainPercent.toInt()}", Modifier.weight(1f)) {
                                it.toFloatOrNull()?.let { v -> gainPercent = v.coerceIn(10f, 500f) }
                            }
                            CompactSettingInput("Silence", silenceThreshold.toString(), Modifier.weight(1f)) {
                                it.toFloatOrNull()?.let { v -> silenceThreshold = v.coerceIn(0.001f, 0.5f) }
                            }
                            CompactSettingInput("Pad ms", "${padMs.toInt()}", Modifier.weight(1f)) {
                                it.toFloatOrNull()?.let { v -> padMs = v.coerceIn(0f, 1000f) }
                            }
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
            val isWorking = s.status == "Composing" || s.status == "Processing audio..." || s.status == "Generating metadata..."
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
                                    isWorking -> PastelOrange.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                s.status.startsWith("Done") -> Icons.Filled.CheckCircle
                                s.status.startsWith("Failed") -> Icons.Filled.Error
                                isWorking -> Icons.Filled.MusicNote
                                else -> Icons.Filled.HourglassBottom
                            },
                            contentDescription = null,
                            tint = when {
                                s.status.startsWith("Done") -> MintGreen
                                s.status.startsWith("Failed") -> MaterialTheme.colorScheme.error
                                isWorking -> PastelOrange
                                else -> TextMuted
                            },
                            modifier = Modifier.size(18.dp)
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
                            isWorking -> PastelOrange
                            else -> TextMuted
                        }
                    )
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(8.dp)) }
    }

    // Hidden WebView — lazy: only created after the user taps "Auto-Create"
    // This avoids loading an entire website on every Studio tab visit.
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

@Composable
private fun SettingInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
private fun CompactSettingInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
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
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("$count", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(4.dp))
            Text(label, color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

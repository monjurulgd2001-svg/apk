package com.zedge.automation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
import com.zedge.automation.ui.theme.BlueSoft
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.MintGreenLight
import com.zedge.automation.ui.theme.PastelOrange
import com.zedge.automation.ui.theme.PastelOrangeLight
import com.zedge.automation.ui.theme.PinkSoft
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.PrimaryPinkLight
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.SkyBlueLight
import com.zedge.automation.ui.theme.SoftRed
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.ui.theme.VioletLight
import com.zedge.automation.ui.theme.VioletSoft
import com.zedge.automation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ── Extra gradient colors for this screen ──
private val NeonCyan = Color(0xFF00D2FF)
private val NeonPurple = Color(0xFF7B2FBE)
private val WarmGold = Color(0xFFFFD700)
private val CoralPink = Color(0xFFFF6B6B)
private val ElectricBlue = Color(0xFF4FACFE)
private val MintSoft = Color(0xFF00F5A0)
private val SunsetOrange = Color(0xFFFF9A56)
private val DeepPurple = Color(0xFF667EEA)
private val HotPink = Color(0xFFFF0080)
private val LimeGreen = Color(0xFF7BED9F)

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
        // ═══════════════════════════════════════════════
        // ── Stable Audio Account Card ──
        // ═══════════════════════════════════════════════
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(NeonPurple, DeepPurple, ElectricBlue))
                        )
                        .border(1.5.dp, Brush.linearGradient(listOf(HotPink, NeonCyan)), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Animated music icon
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(Brush.linearGradient(listOf(HotPink, NeonCyan)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Stable Audio",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Music Generation Engine",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                Modifier
                                    .background(
                                        if (hasToken) MintSoft.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(1.dp, if (hasToken) MintSoft else Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    if (hasToken) "\u2713 ACTIVE" else "NO TOKEN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasToken) MintSoft else Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Status message
                        if (authStatus.isNotBlank()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (authBusy) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonCyan, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        authStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            authError -> CoralPink
                                            authSuccess -> MintSoft
                                            else -> Color.White.copy(alpha = 0.7f)
                                        }
                                    )
                                }
                            }
                        }

                        // Auto-create button
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
                                .height(46.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            )
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (!authBusy) Brush.horizontalGradient(listOf(HotPink, NeonPurple, ElectricBlue))
                                        else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f))),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (authBusy) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        if (authBusy) "Creating account..." else "Auto-Create Account & Sync Token",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════
        // ── Header ──
        // ═══════════════════════════════════════════════
        item {
            Column {
                Text(
                    "\uD83C\uDFB5 Bulk AI Generation",
                    style = MaterialTheme.typography.headlineSmall.merge(
                        TextStyle(brush = Brush.linearGradient(listOf(CoralPink, HotPink, NeonPurple, ElectricBlue, NeonCyan)))
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(140.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(CoralPink, HotPink, NeonPurple, ElectricBlue, NeonCyan)))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "One prompt per line — each one is generated, processed and queued automatically.",
                    color = TextMuted, style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ═══════════════════════════════════════════════
        // ── Main Generation Card ──
        // ═══════════════════════════════════════════════
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(
                                Color(0xFF2D1B4E),
                                Color(0xFF1A2744),
                                Color(0xFF1B3A4B)
                            ))
                        )
                        .border(1.5.dp, Brush.linearGradient(listOf(HotPink, NeonPurple, ElectricBlue)), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Rainbow strip
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Brush.horizontalGradient(listOf(CoralPink, SunsetOrange, WarmGold, LimeGreen, NeonCyan, ElectricBlue, NeonPurple, HotPink)))
                        )

                        // Prompt input
                        OutlinedTextField(
                            bulkPrompts, { bulkPrompts = it },
                            label = { Text("Enter prompts...", color = NeonCyan) },
                            placeholder = {
                                Text("Piano sad emotional melody\nEnergetic electronic dance\nChill lo-fi hip hop beat")
                            },
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = NeonPurple.copy(alpha = 0.5f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f),
                                cursorColor = NeonCyan
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )

                        // Prompt count badge
                        if (promptCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$promptCount prompt${if (promptCount != 1) "s" else ""} ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan, fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Divider
                        Box(
                            Modifier.fillMaxWidth().height(1.dp)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.Transparent)))
                        )

                        // Settings header with icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .background(WarmGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = WarmGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Generation Settings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarmGold
                            )
                        }

                        // Duration
                        ColorfulSettingInput(
                            "Duration (s)",
                            "${duration.toInt()}",
                            "1-180",
                            Modifier.fillMaxWidth(),
                            gradient = Brush.linearGradient(listOf(CoralPink, HotPink))
                        ) {
                            it.toFloatOrNull()?.let { v -> duration = v.coerceIn(1f, 180f) }
                        }

                        // 3 compact fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorfulSettingInput(
                                "Gain %", "${gainPercent.toInt()}", "10-500",
                                Modifier.weight(1f),
                                gradient = Brush.linearGradient(listOf(SunsetOrange, WarmGold))
                            ) {
                                it.toFloatOrNull()?.let { v -> gainPercent = v.coerceIn(10f, 500f) }
                            }
                            ColorfulSettingInput(
                                "Silence", silenceThreshold.toString(), "0.001-0.5",
                                Modifier.weight(1f),
                                gradient = Brush.linearGradient(listOf(ElectricBlue, NeonCyan))
                            ) {
                                it.toFloatOrNull()?.let { v -> silenceThreshold = v.coerceIn(0.001f, 0.5f) }
                            }
                            ColorfulSettingInput(
                                "Pad ms", "${padMs.toInt()}", "0-1000",
                                Modifier.weight(1f),
                                gradient = Brush.linearGradient(listOf(MintGreen, LimeGreen))
                            ) {
                                it.toFloatOrNull()?.let { v -> padMs = v.coerceIn(0f, 1000f) }
                            }
                        }

                        // Auto trim toggle
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(PinkSoft, VioletSoft, BlueSoft)))
                                .border(1.dp, Brush.linearGradient(listOf(HotPink.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f))), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = autoTrimBoost,
                                    onCheckedChange = { autoTrimBoost = it },
                                    colors = CheckboxDefaults.colors(checkedColor = HotPink)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("Auto trim + boost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Apply gain, silence trim & padding", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
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
                                    .height(52.dp)
                                    .shadow(6.dp, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                enabled = promptCount > 0 && !isRunning
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (promptCount > 0 && !isRunning)
                                                Brush.horizontalGradient(listOf(CoralPink, HotPink, NeonPurple, ElectricBlue))
                                            else
                                                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.size(20.dp), tint = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Generate All", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { vm.stopBulkGeneration() },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPink),
                                enabled = isRunning
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(18.dp))
                            }
                        }

                        // Progress bar
                        if (isRunning) {
                            val total = bulkStatuses.size
                            val progress = if (total > 0) (doneCount + failCount).toFloat() / total else 0f
                            Column {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = HotPink,
                                    trackColor = NeonPurple.copy(alpha = 0.2f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${doneCount + failCount}/$total", fontSize = 10.sp, color = TextMuted)
                                    Text("${(progress * 100).toInt()}%", fontSize = 10.sp, color = HotPink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════
        // ── Status Summary Cards ──
        // ═══════════════════════════════════════════════
        if (bulkStatuses.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorfulStatusCard("Done", doneCount, Brush.linearGradient(listOf(MintGreen, MintGreenLight)), Icons.Filled.CheckCircle, Modifier.weight(1f))
                    ColorfulStatusCard("Running", runningCount, Brush.linearGradient(listOf(SunsetOrange, WarmGold)), Icons.Filled.HourglassBottom, Modifier.weight(1f))
                    ColorfulStatusCard("Failed", failCount, Brush.linearGradient(listOf(CoralPink, SoftRed)), Icons.Filled.Error, Modifier.weight(1f))
                }
            }
        }

        // ═══════════════════════════════════════════════
        // ── Status List ──
        // ═══════════════════════════════════════════════
        itemsIndexed(bulkStatuses, key = { i, _ -> i }) { _, s ->
            val isWorking = s.status == "Composing" || s.status == "Processing audio..." || s.status == "Generating metadata..."
            val isDone = s.status.startsWith("Done")
            val isFailed = s.status.startsWith("Failed")

            val cardGradient = when {
                isDone -> Brush.linearGradient(listOf(MintGreen.copy(alpha = 0.08f), MintSoft.copy(alpha = 0.04f)))
                isFailed -> Brush.linearGradient(listOf(CoralPink.copy(alpha = 0.08f), SoftRed.copy(alpha = 0.04f)))
                isWorking -> Brush.linearGradient(listOf(WarmGold.copy(alpha = 0.08f), SunsetOrange.copy(alpha = 0.04f)))
                else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.01f)))
            }
            val borderColor = when {
                isDone -> MintGreen.copy(alpha = 0.3f)
                isFailed -> CoralPink.copy(alpha = 0.3f)
                isWorking -> WarmGold.copy(alpha = 0.3f)
                else -> Color.White.copy(alpha = 0.08f)
            }
            val iconBg = when {
                isDone -> Brush.linearGradient(listOf(MintGreen, MintSoft))
                isFailed -> Brush.linearGradient(listOf(CoralPink, SoftRed))
                isWorking -> Brush.linearGradient(listOf(WarmGold, SunsetOrange))
                else -> Brush.linearGradient(listOf(TextMuted.copy(alpha = 0.3f), TextMuted.copy(alpha = 0.2f)))
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .shadow(4.dp, CircleShape)
                                .background(iconBg, CircleShape),
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
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.prompt,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .background(
                                    when {
                                        isDone -> MintGreen.copy(alpha = 0.15f)
                                        isFailed -> CoralPink.copy(alpha = 0.15f)
                                        isWorking -> WarmGold.copy(alpha = 0.15f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                s.status,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isDone -> MintGreen
                                    isFailed -> CoralPink
                                    isWorking -> WarmGold
                                    else -> TextMuted
                                }
                            )
                        }
                    }
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

// ═══════════════════════════════════════════════
// ── Colorful Setting Input ──
// ═══════════════════════════════════════════════
@Composable
private fun ColorfulSettingInput(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier,
    gradient: Brush,
    onValueChange: (String) -> Unit
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(gradient)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Text(hint, fontSize = 9.sp, color = TextMuted.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = NeonPurple.copy(alpha = 0.4f),
                focusedContainerColor = Color.Black.copy(alpha = 0.15f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f),
                cursorColor = NeonCyan
            )
        )
    }
}

// ═══════════════════════════════════════════════
// ── Colorful Status Card ──
// ═══════════════════════════════════════════════
@Composable
private fun ColorfulStatusCard(
    label: String,
    count: Int,
    gradient: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(gradient)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(gradient, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Column {
                    Text(
                        "$count",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        label,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

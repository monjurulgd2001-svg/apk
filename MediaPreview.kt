package com.zedge.automation.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Brush
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.TextMuted
import kotlinx.coroutines.delay

/**
 * Tiny global audio player for ringtone previews (streams the R2 fileUrl).
 * Tap once to play, tap again to stop. Only one item plays at a time.
 * Exposes real-time progress (0f–1f), positionMs, durationMs for the UI.
 */
object AudioPlayer {
    var playingId    by mutableStateOf<String?>(null)   ; private set
    var isBuffering  by mutableStateOf(false)            ; private set
    var progress     by mutableFloatStateOf(0f)          ; private set
    var positionMs   by mutableIntStateOf(0)             ; private set
    var durationMs   by mutableIntStateOf(0)             ; private set

    private var player: MediaPlayer? = null

    fun toggle(id: String, url: String?) {
        if (url.isNullOrBlank()) return
        if (playingId == id) { stop(); return }
        stop()
        try {
            playingId   = id
            isBuffering = true
            progress    = 0f
            positionMs  = 0
            durationMs  = 0
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    isBuffering = false
                    durationMs  = mp.duration.coerceAtLeast(1)
                    mp.start()
                }
                // Natural completion: release without calling stop()
                setOnCompletionListener { reset() }
                setOnErrorListener { _, _, _ -> stop(); true }
                prepareAsync()
            }
        } catch (_: Exception) { stop() }
    }

    /** Poll current playback position — called from a LaunchedEffect in the UI. */
    fun updateProgress() {
        val mp = player ?: return
        if (!mp.isPlaying) return
        try {
            positionMs = mp.currentPosition
            val dur    = mp.duration.coerceAtLeast(1)
            durationMs = dur
            progress   = positionMs.toFloat() / dur
        } catch (_: Exception) {}
    }

    /** User manually stops. */
    fun stop() {
        try { player?.stop()    } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player      = null
        playingId   = null
        isBuffering = false
        progress    = 0f
        positionMs  = 0
        durationMs  = 0
    }

    /** After natural completion. */
    private fun reset() {
        try { player?.release() } catch (_: Exception) {}
        player      = null
        playingId   = null
        isBuffering = false
        progress    = 0f
        positionMs  = 0
        durationMs  = 0
    }
}

// ── Progress poller ── call this once at any composition root that shows audio cards
// It runs while the composition is active and auto-cancels when removed.
@Composable
fun AudioProgressPoller() {
    LaunchedEffect(AudioPlayer.playingId) {
        while (AudioPlayer.playingId != null) {
            AudioPlayer.updateProgress()
            delay(200)
        }
    }
}

/**
 * Slim animated progress bar shown below a playing audio item.
 * Only renders when [itemId] is the currently playing track.
 */
@Composable
fun AudioProgressBar(itemId: String, modifier: Modifier = Modifier) {
    if (AudioPlayer.playingId != itemId) return

    val animatedProgress by animateFloatAsState(
        targetValue = AudioPlayer.progress,
        animationSpec = tween(durationMillis = 180),
        label = "audioProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress          = { animatedProgress },
            modifier          = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color             = PrimaryPink,
            trackColor        = PrimaryPink.copy(alpha = 0.18f),
            gapSize           = 0.dp
        )
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                formatMsTime(AudioPlayer.positionMs),
                style    = MaterialTheme.typography.labelSmall,
                color    = PrimaryPink,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatMsTime(AudioPlayer.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

/** Converts milliseconds → "m:ss" string (e.g. 93000 → "1:33"). */
private fun formatMsTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    return "$min:%02d".format(sec)
}

/** Fullscreen dark wallpaper preview (tap anywhere or the close button to dismiss). */
@Composable
fun WallpaperPreviewDialog(url: String, title: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6140F11))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable(enabled = false) {}, // prevent dismiss when clicking inside the card
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C25))
            ) {
                Column(Modifier.padding(20.dp)) {
                    // ── Header ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.background(PrimaryPink, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("STAGING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            title.ifBlank { "Err" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(PrimaryPink, CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }
    }
}


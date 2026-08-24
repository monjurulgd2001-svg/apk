package com.zedge.automation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zedge.automation.data.QueueItem
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PastelOrange
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DAY_SLOT_LIMIT = 3
private const val DAYS_PER_PAGE = 28
private const val MAX_PLANNED_DAYS = 700

/**
 * Schedule Calendar — 1:1 port of the web dashboard "Publishing Layout Planner"
 * (buildScheduleCalendar / computeScheduleStats in main.js):
 *
 * - Stats legend: Active DB · Remaining Today · Uploaded Today · Audio Queue · Wallpapers
 * - Days alternate between AUDIO and WALLPAPER day types (3 slots per day)
 * - Today's day type + remaining slots come from Firebase "uploadState"
 *   (lastUploadDate / uploadDayType / totalUploadsToday — written by the dashboard)
 * - Queued items (status == "queued", oldest first) are allocated into future slots:
 *   audio items fill AUDIO days, wallpapers fill WALLPAPER days
 * - Planner extends beyond 28 days until every queued item has a slot (cap 700)
 * - "All Uploads Done! 🎉" when today's 3 uploads are finished
 * - Pagination: 28 days per page (Prev / Next)
 */

private data class ScheduleRule(
    val type: String,
    val remaining: Int,
    val uploadedToday: Int
)

private data class PlannedDay(
    val date: Date,
    val isToday: Boolean,
    val dayType: String, // "AUDIO" | "WALLPAPER"
    val slotCount: Int,
    val slots: List<QueueItem?>
)

/** Mirrors computeScheduleStats() in the web dashboard. */
private fun computeScheduleRule(uploadState: Map<String, Any?>, todayKey: String): ScheduleRule {
    var type = "AUDIO"
    var remaining = DAY_SLOT_LIMIT
    var uploadedToday = 0

    if (uploadState.isNotEmpty()) {
        val lastUploadDate = uploadState["lastUploadDate"] as? String
        val dayType = uploadState["uploadDayType"] as? String
        if (lastUploadDate == todayKey) {
            type = dayType ?: "AUDIO"
            uploadedToday = (uploadState["totalUploadsToday"] as? Number)?.toInt() ?: 0
            remaining = (DAY_SLOT_LIMIT - uploadedToday).coerceAtLeast(0)
        } else {
            val prev = dayType ?: "WALLPAPER"
            type = if (prev == "AUDIO") "WALLPAPER" else "AUDIO"
            uploadedToday = 0
            remaining = DAY_SLOT_LIMIT
        }
    }
    return ScheduleRule(type, remaining, uploadedToday)
}

/** Mirrors the day-building loop of buildScheduleCalendar() in the web dashboard. */
private fun buildPlannedDays(
    queueItems: List<QueueItem>,
    rule: ScheduleRule,
    tz: TimeZone
): List<PlannedDay> {
    val queued = queueItems
        .filter { (it.status ?: "queued") == "queued" }
        .sortedBy { it.createdAt }
    val audios = queued.filter { it.isAudio }
    val wallpapers = queued.filter { !it.isAudio }

    var currentDayType = rule.type
    var audioIdx = 0
    var wallpaperIdx = 0
    var dayIdx = 0
    val daysOut = mutableListOf<PlannedDay>()

    while ((dayIdx < DAYS_PER_PAGE || audioIdx < audios.size || wallpaperIdx < wallpapers.size) && dayIdx < MAX_PLANNED_DAYS) {
        if (dayIdx > 0) {
            currentDayType = if (currentDayType == "AUDIO") "WALLPAPER" else "AUDIO"
        }
        val slotLimit = if (dayIdx == 0) rule.remaining else DAY_SLOT_LIMIT
        val slots = mutableListOf<QueueItem?>()
        repeat(slotLimit) {
            val allocated: QueueItem? = if (currentDayType == "AUDIO") {
                if (audioIdx < audios.size) audios[audioIdx++] else null
            } else {
                if (wallpaperIdx < wallpapers.size) wallpapers[wallpaperIdx++] else null
            }
            slots.add(allocated)
        }
        val cal = Calendar.getInstance(tz).apply { add(Calendar.DAY_OF_YEAR, dayIdx) }
        daysOut.add(
            PlannedDay(
                date = cal.time,
                isToday = dayIdx == 0,
                dayType = currentDayType,
                slotCount = slotLimit,
                slots = slots
            )
        )
        dayIdx++
    }
    return daysOut
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val queueItems by vm.queueItems.collectAsState()
    val uploadState by vm.uploadState.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var page by remember { mutableIntStateOf(1) }

    val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
    // Same "M/d/yyyy" key the web dashboard writes into uploadState.lastUploadDate
    val webDayFmt  = SimpleDateFormat("M/d/yyyy", Locale.US).apply { timeZone = dhakaTz }
    val dayNumFmt  = SimpleDateFormat("d",         Locale.US).apply { timeZone = dhakaTz }
    val dayNameFmt = SimpleDateFormat("EEE",       Locale.US).apply { timeZone = dhakaTz }
    val monthFmt   = SimpleDateFormat("MMM",       Locale.US).apply { timeZone = dhakaTz }
    val headerFmt  = SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = dhakaTz }

    val todayKey = webDayFmt.format(Date())
    val rule = computeScheduleRule(uploadState, todayKey)
    val allDays = buildPlannedDays(queueItems, rule, dhakaTz)

    val queued = queueItems.filter { (it.status ?: "queued") == "queued" }
    val audioQueueCount = queued.count { it.isAudio }
    val wallpaperQueueCount = queued.size - audioQueueCount

    val totalPages = ((allDays.size + DAYS_PER_PAGE - 1) / DAYS_PER_PAGE).coerceAtLeast(1)
    val currentPage = page.coerceIn(1, totalPages)
    val pageDays = allDays.drop((currentPage - 1) * DAYS_PER_PAGE).take(DAYS_PER_PAGE)
    val weeks = pageDays.chunked(7)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { vm.refresh() }
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header
            item {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text("Schedule Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Publishing Layout Planner · 3 uploads/day · Asia/Dhaka", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Stats legend — same five cards as the web dashboard
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(Icons.Filled.Storage,         "Active DB",       activeProject.uppercase(),            PrimaryPink)
                    StatCard(Icons.Filled.HourglassBottom, "Remaining Today", "${rule.remaining} slot(s)",          PastelOrange)
                    StatCard(Icons.Filled.CloudUpload,     "Uploaded Today",  "${rule.uploadedToday} / $DAY_SLOT_LIMIT", MintGreen)
                    StatCard(Icons.Filled.MusicNote,       "Audio Queue",     "$audioQueueCount items",             Violet)
                    StatCard(Icons.Filled.CameraAlt,       "Wallpapers",      "$wallpaperQueueCount items",         SkyBlue)
                }
            }

            // Month header + pagination
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { page = currentPage - 1 },
                        enabled = currentPage > 1,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("‹ Prev") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            headerFmt.format(pageDays.firstOrNull()?.date ?: Date()),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Page $currentPage / $totalPages · ${allDays.size} days planned",
                            fontSize = 10.sp, color = TextMuted
                        )
                    }
                    OutlinedButton(
                        onClick = { page = currentPage + 1 },
                        enabled = currentPage < totalPages,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Next ›") }
                }
            }

            // Day cards — weekly horizontally-scrollable rows
            items(weeks) { week ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    week.forEach { day ->
                        PlannedDayCard(
                            dayNum = dayNumFmt.format(day.date),
                            dayLabel = if (day.isToday) "TODAY" else dayNameFmt.format(day.date).uppercase(),
                            month = monthFmt.format(day.date).uppercase(),
                            day = day
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(30.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
            Column {
                Text(
                    label.uppercase(),
                    fontSize = 8.sp, letterSpacing = 1.sp,
                    color = TextMuted, fontWeight = FontWeight.SemiBold
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlannedDayCard(dayNum: String, dayLabel: String, month: String, day: PlannedDay) {
    val isAudioDay = day.dayType == "AUDIO"
    val badgeColor = if (isAudioDay) Violet else SkyBlue

    Card(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (day.isToday) PrimaryPink.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (day.isToday)
            BorderStroke(2.dp, PrimaryPink)
        else
            BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: big date number + day/month meta + day-type badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        dayNum,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (day.isToday) PrimaryPink else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )
                    Column {
                        Text(
                            dayLabel,
                            fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (day.isToday) PrimaryPink else MaterialTheme.colorScheme.onSurface
                        )
                        Text(month, fontSize = 9.sp, color = TextMuted)
                    }
                }
                Box(
                    Modifier.size(28.dp).background(badgeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isAudioDay) Icons.Filled.MusicNote else Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Day type label
            Text(
                if (isAudioDay) "AUDIO DAY" else "WALLPAPER DAY",
                fontSize = 8.sp, letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold, color = badgeColor
            )

            if (day.slotCount == 0) {
                // Today's 3 uploads already done
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MintGreen.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(14.dp))
                    Text(
                        "All Uploads Done! 🎉",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintGreen, fontWeight = FontWeight.Bold
                    )
                }
            } else {
                day.slots.forEach { item ->
                    if (item != null) SlotItemRow(item) else EmptySlotRow()
                }
            }
        }
    }
}

@Composable
private fun SlotItemRow(item: QueueItem) {
    val isAudio = item.isAudio
    val tint = if (isAudio) Violet else SkyBlue
    Row(
        Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.size(18.dp).background(tint.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isAudio) Icons.Filled.MusicNote else Icons.Filled.CameraAlt,
                contentDescription = null, tint = tint, modifier = Modifier.size(11.dp)
            )
        }
        Text(
            item.title?.trim()?.takeIf { it.isNotBlank() } ?: item.name ?: "Unnamed",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptySlotRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("+", color = TextMuted, style = MaterialTheme.typography.labelSmall)
        Text("Empty slot", color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

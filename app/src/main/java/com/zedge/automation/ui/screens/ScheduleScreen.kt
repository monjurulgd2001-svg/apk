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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zedge.automation.data.QueueItem
import com.zedge.automation.ui.theme.MintGreen
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

private const val SLOT_COUNT = 3

/**
 * Schedule Calendar — redesigned to match a weekly grid layout:
 * - Each day is a card with large date number, day+month label
 * - Today gets a PrimaryPink border highlight
 * - Filled items show audio (Violet) or image (SkyBlue) rows
 * - Empty slots show "+ Empty slot" with a dashed border row
 * - Horizontal scroll per week row
 */
@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val queueItems by vm.queueItems.collectAsState()
    var page by remember { mutableIntStateOf(0) }

    val dhakaTz    = TimeZone.getTimeZone("Asia/Dhaka")
    val dayKeyFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = dhakaTz }
    val dayNumFmt  = SimpleDateFormat("d",          Locale.US).apply { timeZone = dhakaTz }
    val dayNameFmt = SimpleDateFormat("EEE",        Locale.US).apply { timeZone = dhakaTz }
    val monthFmt   = SimpleDateFormat("MMM",        Locale.US).apply { timeZone = dhakaTz }
    val headerFmt  = SimpleDateFormat("MMMM yyyy",  Locale.US).apply { timeZone = dhakaTz }

    val itemsByDay = queueItems
        .filter { it.createdAt > 0 }
        .groupBy { dayKeyFmt.format(Date(it.createdAt)) }

    val cal = Calendar.getInstance(dhakaTz).apply {
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        add(Calendar.DAY_OF_YEAR, page * 28)
    }
    val allDays = (0 until 28).map { offset ->
        Calendar.getInstance(dhakaTz).apply {
            time = cal.time
            add(Calendar.DAY_OF_YEAR, offset)
        }.time
    }
    val today = dayKeyFmt.format(Date())
    val weeks  = allDays.chunked(7)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("Schedule Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Upload schedule · Asia/Dhaka timezone", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Page navigator
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { page-- }, shape = RoundedCornerShape(10.dp)) { Text("‹ Prev") }
                Text(headerFmt.format(allDays[14]), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { page++ }, shape = RoundedCornerShape(10.dp)) { Text("Next ›") }
            }
        }

        // Weekly rows — each horizontally scrollable
        items(weeks) { week ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { day ->
                    val key      = dayKeyFmt.format(day)
                    val dayItems = itemsByDay[key] ?: emptyList()
                    DayCard(
                        dayNum  = dayNumFmt.format(day),
                        dayName = dayNameFmt.format(day).uppercase(),
                        month   = monthFmt.format(day).uppercase(),
                        isToday = key == today,
                        items   = dayItems
                    )
                }
            }
        }

        // Today summary card
        item {
            Spacer(Modifier.height(4.dp))
            val todayItems = itemsByDay[today] ?: emptyList()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today · Dhaka", fontWeight = FontWeight.Bold)
                        Text("${todayItems.size} item(s) in queue", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        Modifier
                            .background(MintGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${todayItems.size}", color = MintGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DayCard(
    dayNum: String, dayName: String, month: String,
    isToday: Boolean, items: List<QueueItem>
) {
    val audioCount = items.count { it.isAudio }
    val imageCount = items.count { !it.isAudio }
    val badgeColor = when {
        items.isEmpty()                    -> TextMuted
        audioCount > 0 && imageCount == 0 -> Violet
        imageCount > 0 && audioCount == 0 -> SkyBlue
        else                               -> PrimaryPink
    }

    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) PrimaryPink.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isToday)
            BorderStroke(2.dp, PrimaryPink)
        else
            BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Date header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    if (isToday) {
                        Text("TODAY", fontSize = 8.sp, color = PrimaryPink, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        dayNum,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isToday) PrimaryPink else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )
                    Text("$dayName · $month", fontSize = 9.sp, color = TextMuted)
                }
                // Type badge
                Box(
                    Modifier.size(28.dp).background(badgeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (audioCount > 0 && imageCount == 0) Icons.Filled.MusicNote else Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Item rows + empty slots
            items.take(SLOT_COUNT).forEach { ItemRow(it) }
            repeat(SLOT_COUNT - items.take(SLOT_COUNT).size) { EmptySlotRow() }
        }
    }
}

@Composable
private fun ItemRow(item: QueueItem) {
    val isAudio  = item.isAudio
    val iconTint = if (isAudio) Violet else SkyBlue
    Row(
        Modifier
            .fillMaxWidth()
            .background(iconTint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (isAudio) Icons.Filled.MusicNote else Icons.Filled.CameraAlt,
            contentDescription = null, tint = iconTint, modifier = Modifier.size(12.dp)
        )
        Text(
            item.title?.takeIf { it.isNotBlank() } ?: item.name ?: "Untitled",
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




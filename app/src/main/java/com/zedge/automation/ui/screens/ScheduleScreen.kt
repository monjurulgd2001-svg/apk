package com.zedge.automation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Schedule Calendar — same as the web dashboard: 28-day pages aligned to
 * weeks, all dates computed in Asia/Dhaka (UTC+6) like dhakaTodayString().
 */
@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val items by vm.queueItems.collectAsState()
    var page by remember { mutableIntStateOf(0) }

    val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = dhakaTz }
    val labelFmt = SimpleDateFormat("d MMM", Locale.US).apply { timeZone = dhakaTz }
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = dhakaTz }

    // Count items per Dhaka day (createdAt)
    val countsByDay = items.filter { it.createdAt > 0 }
        .groupBy { dayFmt.format(Date(it.createdAt)) }
        .mapValues { it.value.size }

    // Build 28 days starting from the beginning of this week + page offset
    val cal = Calendar.getInstance(dhakaTz)
    cal.firstDayOfWeek = Calendar.SUNDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    cal.add(Calendar.DAY_OF_YEAR, page * 28)
    val startDate = cal.time
    val days = (0 until 28).map { offset ->
        val c = Calendar.getInstance(dhakaTz)
        c.time = startDate
        c.add(Calendar.DAY_OF_YEAR, offset)
        c.time
    }
    val today = dayFmt.format(Date())

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("\uD83D\uDCC5 Schedule Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("আপলোড শিডিউল (Asia/Dhaka টাইমজোন) — প্রতিদিনের কিউ আইটেম সংখ্যা", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { page-- }) { Text("\u2039 Prev") }
                Text(monthFmt.format(days[14]), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { page++ }) { Text("Next \u203A") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(days.chunked(7)) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val key = dayFmt.format(day)
                    val count = countsByDay[key] ?: 0
                    val isToday = key == today
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(0.8f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isToday -> PrimaryPink
                                count > 0 -> Color(0x1A1E90FF)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                labelFmt.format(day).split(" ")[0],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (count > 0) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    Modifier.background(if (isToday) Color.White else SkyBlue, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "$count", style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday) PrimaryPink else Color.White, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            val todayCount = countsByDay[today] ?: 0
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today (Dhaka)", fontWeight = FontWeight.SemiBold)
                    Text("$todayCount item(s)", color = MintGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

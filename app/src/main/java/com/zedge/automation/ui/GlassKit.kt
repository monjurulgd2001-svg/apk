package com.zedge.automation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zedge.automation.ui.theme.AccentGradient
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.GlassPanel
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.SkyBlue
import com.zedge.automation.ui.theme.TextMuted
import java.util.Locale

/*
 * GlassKit — reusable glassmorphism UI components.
 * Pure design layer: no business logic lives here.
 */

/** Frosted glass card with a subtle 1px white edge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 22,
    container: Color = GlassPanel,
    borderColor: Color = GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, borderColor)
        ) { content() }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, borderColor)
        ) { content() }
    }
}

/** Section header with a neon accent bar: ▎ TITLE */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    accent: Brush = AccentGradient,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(accent)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.uppercase(Locale.US),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = Color.White,
                maxLines = 1
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        trailing?.invoke()
    }
}

/** Gradient neon action button with a light-catch edge. */
@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    gradient: Brush = AccentGradient,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .border(1.dp, Color(0x59FFFFFF), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Small glowing status dot. */
@Composable
fun GlowDot(color: Color, size: Int = 8) {
    Box(
        Modifier.size((size * 2).dp).background(
            Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent)),
            CircleShape
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(size.dp).background(color, CircleShape))
    }
}

/** Rounded glass icon tile with a tinted edge. */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    size: Int = 44,
    corner: Int = 14
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(bg)
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(corner.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 5 / 10).dp))
    }
}

/** Status shown as a glowing dot + label (replaces the old solid pills). */
@Composable
fun StatusBadge(status: String) {
    val color = when (status.lowercase(Locale.US)) {
        "uploaded", "done", "published" -> MintGreen
        "queued" -> SkyBlue
        else -> TextMuted
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlowDot(color, 6)
        Spacer(Modifier.width(5.dp))
        Text(
            status.uppercase(Locale.US),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

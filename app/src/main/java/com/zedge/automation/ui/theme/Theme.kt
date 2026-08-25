package com.zedge.automation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark "Automation Hub" palette (warm dark + soft pink accent)
val BgColor = Color(0xFF221A1C)          // app background
val TextColor = Color(0xFFF2E9EB)        // primary text
val TextMuted = Color(0xFFA5969A)        // secondary text
val SurfaceColor = Color(0xFF2E2427)     // cards
val BorderColor = Color(0xFF3B3033)
val PrimaryPink = Color(0xFFF27E9D)      // soft pink accent
val PrimaryPinkLight = Color(0xFFFF9EB8)
val SkyBlue = Color(0xFF5E9FE8)
val SkyBlueLight = Color(0xFF8AB9F0)
val Violet = Color(0xFF8854D0)
val VioletLight = Color(0xFFB06AB3)
val MintGreen = Color(0xFF2ECC71)
val MintGreenLight = Color(0xFF26DE81)
val PastelOrange = Color(0xFFF0932B)
val PastelOrangeLight = Color(0xFFFFBE76)
val SoftRed = Color(0xFFE97366)
val HeaderDark = Color(0xFF2A2124)       // top bar / bottom bar
val HeaderDark2 = Color(0xFF332629)

// Soft icon-tile backgrounds (like the stat cards in the reference design)
val VioletSoft = Color(0xFF3A2B4A)
val BlueSoft = Color(0xFF2A3245)
val PinkSoft = Color(0xFF44282F)
val GreenSoft = Color(0xFF23392E)
val ChipBg = Color(0xFF4A3236)
val ChipText = Color(0xFFF2A0B0)
val PillBlueBg = Color(0xFF2A3245)
val PillBlueText = Color(0xFF7FA9F5)
val PillGreenBg = Color(0xFF23392E)
val PillGreenText = Color(0xFF6FD6A0)

val PrimaryGradient = Brush.linearGradient(listOf(PrimaryPinkLight, PrimaryPink))
val SecondaryGradient = Brush.linearGradient(listOf(SkyBlueLight, SkyBlue))
val AccentGradient = Brush.linearGradient(listOf(VioletLight, Violet))
val SuccessGradient = Brush.linearGradient(listOf(MintGreen, MintGreenLight))
val WarningGradient = Brush.linearGradient(listOf(PastelOrangeLight, PastelOrange))
val HeaderGradient = Brush.linearGradient(listOf(HeaderDark, HeaderDark2))

private val DarkColors = darkColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    secondary = SkyBlue,
    tertiary = Violet,
    background = BgColor,
    surface = SurfaceColor,
    onBackground = TextColor,
    onSurface = TextColor,
    surfaceVariant = HeaderDark2,
    onSurfaceVariant = TextMuted,
    outline = BorderColor,
    error = SoftRed
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ZedgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, shapes = AppShapes, content = content)
}

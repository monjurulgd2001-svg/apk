package com.zedge.automation.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

// ── Shared accent colors ──
val PrimaryPink = Color(0xFFF27E9D)
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

// ── Dark palette ──
val BgColor = Color(0xFF221A1C)
val TextColor = Color(0xFFF2E9EB)
val TextMuted = Color(0xFFA5969A)
val SurfaceColor = Color(0xFF2E2427)
val BorderColor = Color(0xFF3B3033)
val HeaderDark = Color(0xFF2A2124)
val HeaderDark2 = Color(0xFF332629)
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

// ── Light palette ──
val LightBg = Color(0xFFF8F5F6)
val LightText = Color(0xFF1C1214)
val LightTextMuted = Color(0xFF7A6B70)
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE8E0E2)
val LightHeader = Color(0xFFF2E9EB)
val LightHeader2 = Color(0xFFF0E4E7)

// ── Gradients ──
val PrimaryGradient = Brush.linearGradient(listOf(PrimaryPinkLight, PrimaryPink))
val SecondaryGradient = Brush.linearGradient(listOf(SkyBlueLight, SkyBlue))
val AccentGradient = Brush.linearGradient(listOf(VioletLight, Violet))
val SuccessGradient = Brush.linearGradient(listOf(MintGreen, MintGreenLight))
val WarningGradient = Brush.linearGradient(listOf(PastelOrangeLight, PastelOrange))
val HeaderGradient = Brush.linearGradient(listOf(HeaderDark, HeaderDark2))

// ── Color schemes ──
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

private val LightColors = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    secondary = SkyBlue,
    tertiary = Violet,
    background = LightBg,
    surface = LightSurface,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = LightHeader2,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    error = SoftRed
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// ── Theme state (survives recomposition) ──
object ThemeState {
    var isDark by mutableStateOf(true)
}

@Composable
fun ZedgeTheme(content: @Composable () -> Unit) {
    val colors = if (ThemeState.isDark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !ThemeState.isDark
        }
    }
    MaterialTheme(colorScheme = colors, shapes = AppShapes, content = content)
}

package com.zedge.automation.ui.theme

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  GLASSMORPHISM DESIGN SYSTEM
//  Dark deep-gradient background · frosted glass panels
//  1px white edges · neon purple/cyan accents
//  (Same val names as before — only the design values changed)
// ═══════════════════════════════════════════════════════════════

// ── Neon accent colors (purple / cyan first) ──
val PrimaryPink = Color(0xFFA855F7)        // neon purple (primary accent)
val PrimaryPinkLight = Color(0xFFC084FC)   // lighter neon purple
val SkyBlue = Color(0xFF22D3EE)            // neon cyan (secondary accent)
val SkyBlueLight = Color(0xFF67E8F9)       // lighter neon cyan
val Violet = Color(0xFF7C3AED)             // deep electric violet
val VioletLight = Color(0xFFA78BFA)        // soft violet glow
val MintGreen = Color(0xFF2EE6A8)          // neon mint (success)
val MintGreenLight = Color(0xFF5EF0C0)
val PastelOrange = Color(0xFFF59E0B)       // amber (warning)
val PastelOrangeLight = Color(0xFFFBBF24)
val SoftRed = Color(0xFFF87171)            // soft neon red (error)

// ── Deep space gradient stops ──
val DeepSpaceTop = Color(0xFF0B0722)       // deep indigo-violet
val DeepSpaceMid = Color(0xFF140B33)       // rich violet night
val DeepSpaceBottom = Color(0xFF061225)    // deep cyan-navy

// ── Glass tokens ──
val GlassPanel = Color(0x14FFFFFF)         // frosted panel fill (~8% white)
val GlassPanelStrong = Color(0x1FFFFFFF)   // stronger frosted fill (~12% white)
val GlassBorder = Color(0x33FFFFFF)        // subtle 1px white edge
val GlassHighlight = Color(0x59FFFFFF)     // top-edge light catch
val NeonPurpleGlow = Color(0x4DA855F7)     // ambient purple glow
val NeonCyanGlow = Color(0x3D22D3EE)       // ambient cyan glow

// ── Dark palette (glass remap — same names) ──
val BgColor = Color(0xFF0B0722)            // fallback behind the gradient
val TextColor = Color(0xFFF1EDFF)
val TextMuted = Color(0xFF9E96C2)
val SurfaceColor = Color(0xF5171233)       // near-opaque glass (dialogs/menus stay readable)
val BorderColor = Color(0x33FFFFFF)        // white hairline outline
val HeaderDark = Color(0x0FFFFFFF)         // frosted header / nav bar fill
val HeaderDark2 = Color(0x1AFFFFFF)
val VioletSoft = Color(0x298B5CF6)         // translucent violet glass tint
val BlueSoft = Color(0x2922D3EE)           // translucent cyan glass tint
val PinkSoft = Color(0x29D946EF)           // translucent magenta glass tint
val GreenSoft = Color(0x292EE6A8)          // translucent mint glass tint
val ChipBg = Color(0x33A855F7)
val ChipText = Color(0xFFD8B4FE)
val PillBlueBg = Color(0x2922D3EE)
val PillBlueText = Color(0xFF67E8F9)
val PillGreenBg = Color(0x292EE6A8)
val PillGreenText = Color(0xFF6EE7B7)

// ── Light palette (kept for the light-mode toggle) ──
val LightBg = Color(0xFFF4F2FB)
val LightText = Color(0xFF17122B)
val LightTextMuted = Color(0xFF6F6890)
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE3DFF2)
val LightHeader = Color(0xFFEDE9FA)
val LightHeader2 = Color(0xFFE6E0F7)

// ── Gradients (neon glass) ──
val PrimaryGradient = Brush.linearGradient(listOf(PrimaryPinkLight, PrimaryPink))
val SecondaryGradient = Brush.linearGradient(listOf(SkyBlueLight, SkyBlue))
val AccentGradient = Brush.linearGradient(listOf(PrimaryPink, SkyBlue))    // purple → cyan neon sweep
val SuccessGradient = Brush.linearGradient(listOf(MintGreen, MintGreenLight))
val WarningGradient = Brush.linearGradient(listOf(PastelOrangeLight, PastelOrange))
val HeaderGradient = Brush.linearGradient(listOf(HeaderDark, HeaderDark2))

// Deep background gradient behind every screen
val AppBackgroundBrush = Brush.verticalGradient(
    listOf(DeepSpaceTop, DeepSpaceMid, DeepSpaceBottom)
)

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

/**
 * Deep-space gradient backdrop with soft neon purple/cyan glow orbs.
 * Purely decorative — wrap the app content in this so translucent
 * frosted panels pick up the glow behind them.
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(if (ThemeState.isDark) AppBackgroundBrush else Brush.verticalGradient(listOf(LightBg, LightHeader)))) {
        if (ThemeState.isDark) {
            Canvas(Modifier.fillMaxSize()) {
                // Neon purple glow — top left
                val purpleCenter = Offset(size.width * 0.12f, size.height * 0.10f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurpleGlow, Color.Transparent),
                        center = purpleCenter,
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = purpleCenter
                )
                // Neon cyan glow — bottom right
                val cyanCenter = Offset(size.width * 0.92f, size.height * 0.88f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyanGlow, Color.Transparent),
                        center = cyanCenter,
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = cyanCenter
                )
                // Faint violet glow — center right
                val violetCenter = Offset(size.width * 0.85f, size.height * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x266D28D9), Color.Transparent),
                        center = violetCenter,
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = violetCenter
                )
            }
        }
        content()
    }
}

@Composable
fun ZedgeTheme(content: @Composable () -> Unit) {
    val colors = if (ThemeState.isDark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = (if (ThemeState.isDark) DeepSpaceTop else LightBg).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !ThemeState.isDark
        }
    }
    MaterialTheme(colorScheme = colors, shapes = AppShapes, content = content)
}

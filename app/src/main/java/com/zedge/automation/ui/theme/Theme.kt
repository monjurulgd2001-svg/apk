package com.zedge.automation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Same palette as style.css (web dashboard)
val BgColor = Color(0xFFF7F9FC)          // --bg-color
val TextColor = Color(0xFF2C3A47)        // --text-color
val TextMuted = Color(0xFF8395A7)        // --text-muted
val SurfaceColor = Color(0xFFFFFFFF)     // --surface-color
val BorderColor = Color(0xFFEEF2F7)      // --border-color
val PrimaryPink = Color(0xFFFF4757)      // watermelon pink
val PrimaryPinkLight = Color(0xFFFF6B8B)
val SkyBlue = Color(0xFF1E90FF)
val SkyBlueLight = Color(0xFF70A1FF)
val Violet = Color(0xFF8854D0)
val VioletLight = Color(0xFFA55EEA)
val MintGreen = Color(0xFF2ECC71)
val MintGreenLight = Color(0xFF26DE81)
val PastelOrange = Color(0xFFF0932B)
val PastelOrangeLight = Color(0xFFFFBE76)
val SoftRed = Color(0xFFEE5253)
val HeaderDark = Color(0xFF1E272E)
val HeaderDark2 = Color(0xFF2C3E50)

val PrimaryGradient = Brush.linearGradient(listOf(PrimaryPinkLight, PrimaryPink))
val SecondaryGradient = Brush.linearGradient(listOf(SkyBlueLight, SkyBlue))
val AccentGradient = Brush.linearGradient(listOf(VioletLight, Violet))
val SuccessGradient = Brush.linearGradient(listOf(MintGreen, MintGreenLight))
val WarningGradient = Brush.linearGradient(listOf(PastelOrangeLight, PastelOrange))
val HeaderGradient = Brush.linearGradient(listOf(HeaderDark, HeaderDark2))

private val LightColors = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    secondary = SkyBlue,
    tertiary = Violet,
    background = BgColor,
    surface = SurfaceColor,
    onBackground = TextColor,
    onSurface = TextColor,
    outline = BorderColor,
    error = SoftRed
)

// Same rounded feel as --radius-lg/md/sm
private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ZedgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, shapes = AppShapes, content = content)
}

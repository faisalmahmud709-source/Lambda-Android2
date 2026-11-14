package com.lambda.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** "Cyber-Midnight" palette, ported from the web app's CSS. */
object LC {
    val Bg = Color(0xFF0C0818)
    val Sidebar = Color(0xFF090614)
    val Card = Color(0xFF150F28)
    val Elevated = Color(0xFF1D1538)
    val Field = Color(0xFF141417)
    val Primary = Color(0xFFF0BC68)       // amber
    val OnPrimary = Color(0xFF181308)
    val Text = Color(0xFFF4F4F7)
    val Muted = Color(0xFF9B98AB)
    val Purple = Color(0xFFA855F7)
    val Border = Color(0x26A855F7)
    val BorderStrong = Color(0x4DA855F7)
    val White05 = Color(0x0DFFFFFF)
    val White10 = Color(0x1AFFFFFF)
    val Red = Color(0xFFF87171)
    val Emerald = Color(0xFF34D399)
    val Amber = Color(0xFFFCD34D)
    val Indigo = Color(0xFF818CF8)
    val Blue = Color(0xFF60A5FA)

    val CardBrush = Brush.verticalGradient(listOf(Color(0xD917102C), Color(0xEB100B20)))
    val ChartColors = listOf(
        Color(0xFFF0BC68), Color(0xFF60A5FA), Color(0xFF34D399), Color(0xFFA78BFA),
        Color(0xFFF87171), Color(0xFF38BDF8), Color(0xFFFBBF24)
    )
}

val DisplayFont = FontFamily.Serif
val MonoFont = FontFamily.Monospace

private val scheme = darkColorScheme(
    primary = LC.Primary,
    onPrimary = LC.OnPrimary,
    secondary = LC.Purple,
    background = LC.Bg,
    onBackground = LC.Text,
    surface = LC.Card,
    onSurface = LC.Text,
    surfaceVariant = LC.Elevated,
    onSurfaceVariant = LC.Muted,
    outline = LC.BorderStrong,
    error = LC.Red,
    surfaceContainer = LC.Elevated,
    surfaceContainerHigh = LC.Elevated,
    surfaceContainerHighest = LC.Elevated
)

private val typography = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, color = LC.Text),
    bodyMedium = TextStyle(fontSize = 13.sp, color = LC.Text),
    bodySmall = TextStyle(fontSize = 12.sp, color = LC.Muted),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontStyle = FontStyle.Italic, fontSize = 28.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = MonoFont, fontSize = 10.sp, letterSpacing = 1.4.sp)
)

@Composable
fun LambdaTheme(content: @Composable () -> Unit) {
    // The app is dark-only by design (same as the web version).
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

package com.lambda.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LambdaDark = darkColorScheme(
    primary = Color(0xFFF0BC68), onPrimary = Color(0xFF181308),
    background = Color(0xFF0C0818), onBackground = Color(0xFFF4F4F7),
    surface = Color(0xFF150F28), onSurface = Color(0xFFF4F4F7),
    surfaceVariant = Color(0xFF1F183C), onSurfaceVariant = Color(0xFFB8B3C9),
    secondary = Color(0xFF9B8AD8), outline = Color(0xFF403653)
)
private val LambdaLight = lightColorScheme(primary = Color(0xFF8A5A16), secondary = Color(0xFF66539A), background = Color(0xFFF7F5FA), surface = Color.White)
@Composable fun LambdaTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) LambdaDark else LambdaLight, typography = Typography(), content = content)
}

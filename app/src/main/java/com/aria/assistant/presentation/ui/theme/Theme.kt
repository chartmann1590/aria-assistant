package com.aria.assistant.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AriaDarkColorScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1A6E),
    onPrimaryContainer = AuroraLavender,
    secondary = AuroraTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF083A36),
    onSecondaryContainer = AuroraTeal,
    tertiary = AuroraAmber,
    onTertiary = Color.Black,
    background = NebulaBase,
    onBackground = TextPrimary,
    surface = NebulaBase1,
    onSurface = TextPrimary,
    surfaceVariant = NebulaBase2,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = GlassStroke,
    error = Color(0xFFEF4444),
    onError = Color.White,
)

@Composable
fun AriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AriaDarkColorScheme,
        typography = AriaTypography,
        content = content
    )
}

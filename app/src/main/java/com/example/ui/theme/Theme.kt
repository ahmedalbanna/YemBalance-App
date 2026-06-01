package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NeonTeal,
    onPrimary = Color.White,
    secondary = NeonBlue,
    onSecondary = Color.White,
    background = CyberObsidian,
    onBackground = TextLight,
    surface = CardSlate,
    onSurface = TextLight,
    outline = BorderDark,
    surfaceVariant = CyberObsidian,
    onSurfaceVariant = TextMuted,
    error = DarkRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}


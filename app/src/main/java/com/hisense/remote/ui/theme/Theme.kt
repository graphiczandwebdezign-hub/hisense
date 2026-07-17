package com.hisense.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1557B0),
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color.White,
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF1A1D27),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF222533),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFF333650),
    error = Color(0xFFEA4335),
    onError = Color.White,
)

@Composable
fun HisenseRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}

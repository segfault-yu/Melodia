package com.lin0721.linmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceLight,
    onBackground = TextGray,
    onSurface = TextGray
)

// Melodia 应用的全局 Material3 主题配置
@Composable
fun MelodiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
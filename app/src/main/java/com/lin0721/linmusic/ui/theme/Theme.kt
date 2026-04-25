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

@Composable
fun LinMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
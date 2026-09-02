package com.lin0721.linmusic.core.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.lin0721.linmusic.core.ui.interaction.MelodiaPressIndication

private val DarkColorScheme = darkColorScheme(
    primary = NeteaseRed,
    onPrimary = Color.White,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextGray,
    error = NeteaseRed,
    onError = Color.White
)

// Melodia 应用的全局 Material3 主题配置
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelodiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = MelodiaShapes
    ) {
        CompositionLocalProvider(
            LocalIndication provides MelodiaPressIndication.Default,
            LocalRippleConfiguration provides null,
            content = content
        )
    }
}
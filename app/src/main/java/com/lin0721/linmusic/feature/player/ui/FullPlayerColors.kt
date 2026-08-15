package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import com.lin0721.linmusic.core.ui.theme.ColorPalette

// 全屏播放器的整套配色，均由封面提取出的主色推导
data class FullPlayerColors(
    val dominant: Color,
    val secondary: Color,
    val tintedCard: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val accent: Color,
    val lyricsHighlight: Color
)

// 在 HSV 空间上改写主色，由调用方决定饱和度与明度的取值
private fun deriveHsvColor(source: Color, transform: (FloatArray) -> Unit): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (source.red * 255).toInt(),
        (source.green * 255).toInt(),
        (source.blue * 255).toInt(),
        hsv
    )
    transform(hsv)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// 主色切换时平滑过渡，并派生出渐变、强调与歌词高亮色
@Composable
fun rememberFullPlayerColors(palette: ColorPalette): FullPlayerColors {
    val animatedDominant by animateColorAsState(
        targetValue = palette.dominant,
        animationSpec = tween(800),
        label = "bg_dominant"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(800),
        label = "bg_secondary"
    )

    val tintedCardColor = animatedDominant.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface)

    // 灰度封面（饱和度极低）走纯灰阶分支，避免凭空染上偏色
    val gradientStart = remember(animatedDominant) {
        deriveHsvColor(animatedDominant) { hsv ->
            if (hsv[1] < 0.05f) {
                hsv[1] = 0f
                hsv[2] = 0.15f
            } else {
                hsv[1] = (hsv[1] * 0.6f).coerceIn(0.35f, 1f)
                hsv[2] = 0.92f
            }
        }
    }

    val gradientEnd = remember(animatedDominant) {
        deriveHsvColor(animatedDominant) { hsv ->
            if (hsv[1] < 0.05f) {
                hsv[1] = 0f
                hsv[2] = 0.05f
            } else {
                hsv[1] = (hsv[1] + 0.35f).coerceIn(0.75f, 1f)
                hsv[2] = 0.3f
            }
        }
    }

    val accentColor = remember(animatedDominant) {
        deriveHsvColor(animatedDominant) { hsv ->
            if (hsv[1] < 0.05f) {
                hsv[1] = 0f
                hsv[2] = 0.3f
            } else {
                hsv[1] = 1.0f
                hsv[2] = 0.75f
            }
        }
    }

    val lyricsHighlight = lerp(start = animatedDominant, stop = Color.White, fraction = 0.85f)

    return FullPlayerColors(
        dominant = animatedDominant,
        secondary = animatedSecondary,
        tintedCard = tintedCardColor,
        gradientStart = gradientStart,
        gradientEnd = gradientEnd,
        accent = accentColor,
        lyricsHighlight = lyricsHighlight
    )
}

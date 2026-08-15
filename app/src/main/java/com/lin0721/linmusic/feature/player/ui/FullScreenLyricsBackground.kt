package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 全屏歌词页流体渐变蒙层：纯色底 + 两枚缓慢游走的径向光晕
// 动画值只在 drawBehind 内读取，保证每帧仅触发绘制阶段而不重组
@Composable
fun Modifier.fullScreenLyricsBackground(
    gradientStart: Color,
    gradientEnd: Color,
    accentColor: Color
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_mesh_fullscreen")

    val accentCenterX by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_x"
    )
    val accentCenterY by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_y"
    )
    val accentRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_radius"
    )

    val whiteCenterX by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_x"
    )
    val whiteCenterY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_y"
    )
    val whiteRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_radius"
    )

    return this.drawBehind {
        val baseSize = size.minDimension
        drawRect(color = gradientEnd)

        val accentRadius = baseSize * accentRadiusScale
        val accentCenter = Offset(size.width * accentCenterX, size.height * accentCenterY)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.6f), Color.Transparent),
                center = accentCenter,
                radius = accentRadius
            ),
            center = accentCenter,
            radius = accentRadius
        )

        val whiteRadius = baseSize * whiteRadiusScale
        val whiteCenter = Offset(size.width * whiteCenterX, size.height * whiteCenterY)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gradientStart.copy(alpha = 0.5f), Color.Transparent),
                center = whiteCenter,
                radius = whiteRadius
            ),
            center = whiteCenter,
            radius = whiteRadius
        )
    }
}

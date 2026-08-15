package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// 跟随封面滚动位移的主色渐变背景，渐变终点做缓慢呼吸动画
// 位移用 provider 延迟到绘制层读取，避免滚动时触发整屏重组
@Composable
fun FullPlayerBackground(
    dominant: Color,
    translationYProvider: () -> Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_breathe")
    val gradientEndY by infiniteTransition.animateFloat(
        initialValue = 2400f,
        targetValue = 3000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_end"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1200.dp)
            .graphicsLayer {
                translationY = translationYProvider()
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominant,
                        dominant.copy(alpha = 0.75f),
                        dominant.copy(alpha = 0.5f),
                        dominant.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = gradientEndY
                )
            )
    )
}

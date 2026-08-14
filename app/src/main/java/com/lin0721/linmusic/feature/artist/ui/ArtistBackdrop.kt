package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.ui.theme.extractDominantColor

// 顶部大图背景层：随列表滚动做视差平移、拉伸与渐隐，并回传封面主色
@Composable
fun BoxScope.ArtistBackdrop(
    artist: ArtistDetailInfo,
    progress: Float,
    collapseThresholdPx: Float,
    dominantColor: Color,
    onDominantColorChange: (Color) -> Unit
) {
    val bgScale = 1f + (1f - progress) * 0.1f // 微微视差拉伸

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .graphicsLayer {
                translationY = -progress * collapseThresholdPx * 0.5f // 视差平移
                scaleX = bgScale
                scaleY = bgScale
                alpha = 1f - progress * 0.7f // 渐隐
            }
    ) {
        val bgUrl = artist.cover.ifEmpty { artist.avatar }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (bgUrl.isNotEmpty()) "$bgUrl?param=640y640" else "")
                .allowHardware(false)
                .crossfade(true)
                .build(),
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                onDominantColorChange(extractDominantColor(state.result.drawable))
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // dominantColor 独立底色层：压在封面图上方，高度延伸到操作栏底部，与 Tab 栏接在一起
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .align(Alignment.TopCenter)
            .graphicsLayer {
                translationY = -progress * collapseThresholdPx * 0.5f // 与大图同步视差平移
                alpha = 1f - progress * 0.7f // 与大图同步渐隐
            }
    ) {
        // 纯色底色向下延伸,底边缘与底部的 tab_bar 接在一起
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(dominantColor)
        )
    }
}

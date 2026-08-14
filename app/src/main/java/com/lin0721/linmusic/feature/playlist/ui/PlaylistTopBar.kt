package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// ────────────────────────────────────────────────────────────────────────────
// 固定 Overlay 顶栏
// 进入时背景 alpha=0（完全透明，视觉上不存在）
// 滚动后 alpha 随 progress 增大直到完全不透明
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun PlaylistTopBar(
    title: String,
    progress: Float,
    overlayHeight: Dp,
    statusBarHeight: Dp,
    dominantColor: Color,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(overlayHeight)
            .background(dominantColor.copy(alpha = progress))
            .padding(top = statusBarHeight) // 内容区域被挤到状态栏下方
            .zIndex(8f)
    ) {
        // 返回键
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MelodiaSpacing.xs)
        ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back",
                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
        }
        // 歌单名称：随着滚动淡入及向上微移
        val titleAlpha = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
        val titleOffsetY = lerp(8.dp, 0.dp, titleAlpha)
        Text(
            text       = title,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize   = 17.sp,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 60.dp)
                .offset(y = titleOffsetY)
                .alpha(titleAlpha)
        )
    }
}

// 折叠到临界点后吸附在顶栏下沿的播放按钮
@Composable
fun BoxScope.PlaylistCollapsedPlayFab(
    progress: Float,
    overlayHeight: Dp,
    onPlayAll: () -> Unit
) {
    val fabScale = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
    if (fabScale > 0f) {
        FloatingActionButton(
            onClick        = onPlayAll,
            containerColor = MaterialTheme.colorScheme.primary,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.TopEnd)
                .padding(end = MelodiaSpacing.md)
                .offset(y = overlayHeight - 28.dp)
                .size(56.dp)
                .zIndex(10f)
                .graphicsLayer(
                    scaleX = fabScale,
                    scaleY = fabScale,
                    alpha = fabScale
                )
                .shadow(8.dp * fabScale, CircleShape)
        ) {
            Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
        }
    }
}

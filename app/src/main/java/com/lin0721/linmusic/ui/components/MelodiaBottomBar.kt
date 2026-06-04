package com.lin0721.linmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.lin0721.linmusic.Screen
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.TextGray
import com.lin0721.linmusic.ui.theme.ColorPalette
import com.lin0721.linmusic.ui.theme.extractColorPalette
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState

//悬浮播放控制卡片

@Composable
fun MiniPlayerCard(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: ((Float) -> Unit)? = null
) {
    if (currentTrack == null) return

    val context = LocalContext.current
    val artworkRequest = remember(currentTrack.mediaMetadata.artworkUri) {
        val cleanUrl = currentTrack.mediaMetadata.artworkUri?.toString()
            ?.replace("?param=130y130", "")
            ?.replace("?param=200y200", "")
            ?.replace("?param=300y300", "")
            ?: ""
        ImageRequest.Builder(context)
            .data(cleanUrl.ifEmpty { null })
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

    // 缓存并提取主色调，默认为深灰色
    var colorPalette by remember(currentTrack) {
        mutableStateOf(ColorPalette(Color(0xFF333333), Color(0xFF222222)))
    }
    
    // 平滑过渡主色调变化
    val animatedDominant by animateColorAsState(
        targetValue = colorPalette.dominant,
        animationSpec = tween(800),
        label = "mini_player_dominant"
    )
    
    // 采用与播放界面歌词卡片一致的算法

    val cardBackgroundColor = remember(animatedDominant) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (animatedDominant.red * 255).toInt(),
            (animatedDominant.green * 255).toInt(),
            (animatedDominant.blue * 255).toInt(),
            hsv
        )
        hsv[1] = (hsv[1] + 0.35f).coerceIn(0.75f, 1f)
        hsv[2] = 0.3f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    onDrag?.invoke(delta)
                },
                onDragStopped = { velocity ->
                    onDragEnd?.invoke(velocity)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // 专辑封面
                AsyncImage(
                    model = artworkRequest,
                    contentDescription = null,
                    onSuccess = { state ->
                        colorPalette = extractColorPalette(state.result.drawable)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                // 歌名和歌手信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack.mediaMetadata.title?.toString() ?: "未知歌名",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTrack.mediaMetadata.artist?.toString() ?: "未知歌手",
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 播放/暂停按钮
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // 下一首按钮
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 底部进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(NeteaseRed)
                )
            }
        }
    }
}

//底部导航栏  
@Composable
fun MelodiaNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onCreateClick: () -> Unit,
    isCreateMenuOpen: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BackgroundDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                Triple("主页", Icons.Default.Home, Screen.Home),
                Triple("搜索", Icons.Default.Search, Screen.Search),
                Triple("音乐库", Icons.Default.LibraryMusic, Screen.Library),
                Triple("创建", if (isCreateMenuOpen) Icons.Rounded.Close else Icons.Default.AddBox, null)
            )

            navItems.forEach { (label, icon, targetScreen) ->
                val isSelected = targetScreen != null && currentScreen == targetScreen
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (targetScreen != null) {
                                onNavigate(targetScreen)
                            } else {
                                onCreateClick()
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 药丸形状背景
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF383A4A) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else TextGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else TextGray,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

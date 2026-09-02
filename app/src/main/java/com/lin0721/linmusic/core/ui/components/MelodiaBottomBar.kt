package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.Screen
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MelodiaPress
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.ColorPalette
import com.lin0721.linmusic.core.ui.theme.extractColorPalette
import com.lin0721.linmusic.core.ui.theme.PaletteMemoryCache
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import com.lin0721.linmusic.core.ui.theme.FallbackDominant
import com.lin0721.linmusic.core.ui.theme.FallbackSecondary
import com.lin0721.linmusic.core.ui.theme.NavPillSelected
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.InfoCardRadius
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.feature.player.ui.deviceIcon
import com.lin0721.linmusic.feature.player.ui.deviceLabel
import com.lin0721.linmusic.feature.player.ui.rememberCurrentOutputDevice

//悬浮播放控制卡片

@Composable
fun MiniPlayerCard(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
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

    // 缓存并提取主色调，优先从缓存中获取，无缓存则默认为深灰色
    var colorPalette by remember(currentTrack.mediaId) {
        mutableStateOf(
            PaletteMemoryCache.get(currentTrack.mediaId) ?: ColorPalette(FallbackDominant, FallbackSecondary)
        )
    }
    
    // 平滑过渡主色调变化
    val animatedDominant by animateColorAsState(
        targetValue = colorPalette.dominant,
        animationSpec = tween(800),
        label = "mini_player_dominant"
    )

    val cardBackgroundColor = remember(animatedDominant) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (animatedDominant.red * 255).toInt(),
            (animatedDominant.green * 255).toInt(),
            (animatedDominant.blue * 255).toInt(),
            hsv
        )
        if (hsv[1] < 0.05f) {
            // 如果是灰度中性色，保持灰度，亮度设为适合底栏的深色
            hsv[1] = 0f
            hsv[2] = 0.15f
        } else {
            hsv[1] = (hsv[1] + 0.35f).coerceIn(0.75f, 1f)
            hsv[2] = 0.3f
        }
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(InfoCardRadius))
            .background(cardBackgroundColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(InfoCardRadius))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    onDrag?.invoke(delta)
                },
                onDragStopped = { velocity ->
                    onDragEnd?.invoke(velocity)
                }
            )
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            ) {
                // 专辑封面
                SubcomposeAsyncImage(
                    model = artworkRequest,
                    contentDescription = null,
                    onSuccess = { state ->
                        val palette = extractColorPalette(state.result.drawable)
                        colorPalette = palette
                        PaletteMemoryCache.put(currentTrack.mediaId, palette)
                    },
                    loading = { CoverPlaceholder() },
                    error = { CoverPlaceholder() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(RadiusCompact)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                // 歌名歌手信息；连接非扬声器设备时第二行由歌手名切换成设备名，扬声器播放时不显示第二行
                val connectedDevice = rememberCurrentOutputDevice()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    val title = currentTrack.mediaMetadata.title?.toString() ?: "未知歌名"
                    val artist = currentTrack.mediaMetadata.artist?.toString().orEmpty()
                    val titleLine = if (connectedDevice != null && artist.isNotBlank()) "$title · $artist" else title
                    Text(
                        text = titleLine,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.basicMarquee()
                    )
                    if (connectedDevice != null) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = deviceIcon(connectedDevice.type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = deviceLabel(connectedDevice),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 13.sp
                                )
                            )
                        }
                    } else if (artist.isNotBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = artist,
                            color = TextGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 14.sp
                            )
                        )
                    }
                }

                // 播放/暂停按钮
                MelodiaIconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // 下一首按钮
                MelodiaIconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 底部进度条
            MiniPlayerProgress(
                currentPositionProvider = currentPositionProvider,
                duration = duration
            )
        }
    }
}

// 进度条组件
@Composable
fun MiniPlayerProgress(
    currentPositionProvider: () -> Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        val currentPosition = currentPositionProvider()
        val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(NeteaseRed)
        )
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
                .height(60.dp)
                .padding(top = 12.dp, bottom = 2.dp),
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
                        .pressable(MelodiaPress.Tab) {
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
                            .background(if (isSelected) NavPillSelected else Color.Transparent)
                            .padding(horizontal = MelodiaSpacing.md, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else TextGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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

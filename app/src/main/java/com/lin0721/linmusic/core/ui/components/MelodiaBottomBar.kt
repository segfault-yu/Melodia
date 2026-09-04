package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import coil.compose.AsyncImage
import com.lin0721.linmusic.Screen
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.extractBackdropPalette
import com.lin0721.linmusic.core.ui.theme.PaletteMemoryCache
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import com.lin0721.linmusic.core.ui.theme.FallbackBackdropPalette
import com.lin0721.linmusic.core.ui.theme.NavPillSelected
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.InfoCardRadius
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.darken
import com.lin0721.linmusic.core.ui.theme.lighten
import com.lin0721.linmusic.feature.player.ui.deviceIcon
import com.lin0721.linmusic.feature.player.ui.deviceLabel
import com.lin0721.linmusic.feature.player.ui.drawSingleHueMesh
import com.lin0721.linmusic.feature.player.ui.rememberCurrentOutputDevice

// 光斑位置固定不做动画，跟大卡片/全屏背景那种游走效果区分开，
// 避免小尺寸下持续重绘、观感也容易显得杂
private val MINI_PLAYER_BLUR_RADIUS = 28.dp

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
            // 封面显示尺寸只有 44dp，Coil 默认按控件尺寸解码位图，喂给 Palette 的像素太少，
            // 跟全屏播放器（封面接近整屏宽度）解码出来的位图不是一回事，取色结果会不一致。
            // 固定一个跟显示尺寸无关的解码尺寸，两边取色才可比
            .size(coil.size.Size(300, 300))
            .build()
    }

    // 缓存并提取背景色，优先从缓存中获取，无缓存则默认为深灰色
    var colorPalette by remember(currentTrack.mediaId) {
        mutableStateOf(
            PaletteMemoryCache.get(currentTrack.mediaId) ?: FallbackBackdropPalette
        )
    }

    // 平滑过渡背景色变化；base 取自 Vibrant，本身偏亮，卡片背景需要压暗一档才不会太扎眼
    val animatedBase by animateColorAsState(
        targetValue = colorPalette.base,
        animationSpec = tween(800),
        label = "mini_player_base"
    )
    val fillColor = remember(animatedBase) { animatedBase.darken(0.35f) }
    val lightBlob = remember(animatedBase) { animatedBase.lighten(0.05f) }
    val darkBlob = remember(animatedBase) { animatedBase.darken(0.15f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(InfoCardRadius))
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(MINI_PLAYER_BLUR_RADIUS)
                .drawBehind {
                    val baseSize = size.minDimension
                    drawSingleHueMesh(
                        fill = fillColor,
                        lightBlob = lightBlob,
                        lightCenter = Offset(size.width * 0.15f, size.height * 0.2f),
                        lightRadius = baseSize * 0.9f,
                        darkBlob = darkBlob,
                        darkCenter = Offset(size.width * 0.95f, size.height * 1.0f),
                        darkRadius = baseSize * 0.6f
                    )
                }
        )
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                // 专辑封面
                AsyncImage(
                    model = artworkRequest,
                    contentDescription = null,
                    onSuccess = { state ->
                        val palette = extractBackdropPalette(state.result.drawable)
                        colorPalette = palette
                        PaletteMemoryCache.put(currentTrack.mediaId, palette)
                    },
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

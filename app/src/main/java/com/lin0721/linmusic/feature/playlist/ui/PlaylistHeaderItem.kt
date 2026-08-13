package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.extractDominantColor
import com.lin0721.linmusic.core.model.PlaylistDetail

// ────────────────────────────────────────────────────────────────────────────
// 全出血 Hero（LazyColumn Item 1）
// 封面区包含 overlayHeight 的内边距，使封面在透明 overlay 下方自然显示
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun PlaylistHeaderItem(
    playlist: PlaylistDetail,
    coverSize: Dp,
    coverAlpha: Float,
    isCollapsed: Boolean,
    statusBarHeight: Dp,
    dominantColor: Color,
    onColorCalculated: (Color) -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onMoreClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    selectedHistoryDate: String = "今天"
) {
    val todayStr = remember {
        java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.getDefault()).format(java.util.Date())
    }
    val displayDateStr = remember(selectedHistoryDate) {
        if (selectedHistoryDate == "今天") {
            todayStr
        } else {
            val parts = selectedHistoryDate.split("-")
            if (parts.size == 3) {
                "${parts[0]}年${parts[1]}月${parts[2]}日"
            } else {
                selectedHistoryDate
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 使用从封面提取的主色调渐变到背景黑
            .background(Brush.verticalGradient(listOf(dominantColor, MaterialTheme.colorScheme.background)))
    ) {
        // 封面：与操作区的返回键水平对齐
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight + MelodiaSpacing.md, bottom = MelodiaSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val coverRequest = remember(playlist.coverImgUrl) {
                ImageRequest.Builder(context)
                    .data("${playlist.coverImgUrl}?param=400y400")
                    .allowHardware(false)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model              = coverRequest,
                contentDescription = playlist.name,
                contentScale       = ContentScale.Crop,
                onSuccess          = { state ->
                    onColorCalculated(extractDominantColor(state.result.drawable))
                },
                modifier           = Modifier
                    .size(coverSize)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(10.dp), clip = false)
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (coverAlpha < 1f) Modifier.alpha(coverAlpha) else Modifier)
            )
        }

        // 歌单信息与操作行：折叠后隐藏
        if (!isCollapsed) {
            Column(modifier = Modifier.padding(horizontal = MelodiaSpacing.md)) {
                Text(playlist.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                    fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (playlist.id == -1L) {
                    Text(displayDateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else if (playlist.id == -2L) {
                    Text("网易云个人听歌记录统计", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (playlist.creator != null) {
                            AsyncImage(
                                model = "${playlist.creator.avatarUrl}?param=50y50",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(playlist.creator.nickname, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("为你打造", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    if (!playlist.description.isNullOrBlank()) {
                        Spacer(Modifier.height(MelodiaSpacing.xs))
                        Text(playlist.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(MelodiaSpacing.xs))
                val playCountText = if (playlist.playCount > 0) " • 播放 ${formatPlayCount(playlist.playCount)} 次" else ""
                Text("${playlist.tracks.size} 首歌曲$playCountText", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (playlist.id != -1L && playlist.id != -2L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MelodiaSpacing.md, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSubscribeClick) {
                            Icon(
                                imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = if (isSubscribed) "已收藏" else "收藏",
                                tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onCommentsClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Comment,
                                contentDescription = "评论",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onShufflePlay,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, "Shuffle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        FloatingActionButton(
                            onClick        = onPlayAll,
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape          = CircleShape,
                            modifier       = Modifier
                                .size(56.dp)
                                .shadow(6.dp, CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

package com.lin0721.linmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.SpotifyGreen
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.SurfaceLight
import com.lin0721.linmusic.ui.theme.TextGray

@Composable
fun MiniPlayer(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) return

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth().height(64.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = currentTrack.mediaMetadata.artworkUri,
                contentDescription = "Now Playing",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack.mediaMetadata.title?.toString() ?: "Unknown Title",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { }) { Icon(Icons.Default.Phone, "Devices", tint = Color.White, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onTogglePlay) {
                if (isPlaying) {
                    Text("⏸", color = Color.White, fontSize = 24.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun FullPlayerScreen(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit
) {
    if (currentTrack == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF4A3026), BackgroundDark),
                    startY = 0f, endY = 1500f
                )
            )
            .padding(24.dp)
    ) {
        // 顶部控制
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text("NOW PLAYING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 封面图 (高分辨率 - 如果网易云 API 有带 param 就去掉或增大，但是 artworkUri 已经是 URL 了)
        val coverUrl = currentTrack.mediaMetadata.artworkUri?.toString()?.replace("?param=300y300", "") ?: ""
        AsyncImage(
            model = coverUrl.ifEmpty { "https://picsum.photos/seed/nowplaying/400" },
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 歌曲信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = currentTrack.mediaMetadata.title?.toString() ?: "Unknown",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack.mediaMetadata.artist?.toString() ?: "Unknown",
                    color = TextGray,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 进度条（Mock）
        Slider(
            value = 0.0f,
            onValueChange = {},
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = SurfaceLight
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0:00", color = TextGray, fontSize = 12.sp)
            Text("0:00", color = TextGray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 播放控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Shuffle", tint = TextGray, modifier = Modifier.size(24.dp))
            Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
            FloatingActionButton(
                onClick = onTogglePlay,
                containerColor = SpotifyGreen,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                if (isPlaying) {
                    Text("⏸", color = Color.Black, fontSize = 32.sp)
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(40.dp))
                }
            }
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
            Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = TextGray, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

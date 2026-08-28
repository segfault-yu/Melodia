package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// ────────────────────────────────────────────────────────────────────────────
// 推荐歌曲板块（标题行 + 可一键加入歌单的推荐列表）
// ────────────────────────────────────────────────────────────────────────────
fun LazyListScope.playlistRecommendItems(
    recommendedSongs: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onRefreshRecommendations: () -> Unit,
    onPlaySong: (Track) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAddRecommendSong: (Track) -> Unit
) {
    item(key = "recommendation_header") {
        RecommendationHeader(
            onRefresh = onRefreshRecommendations
        )
    }
    items(recommendedSongs, key = { "rec_${it.id}" }) { track ->
        SongRow(
            data = SongRowData(
                id = track.id,
                title = track.name,
                artist = track.ar.joinToString(" • ") { it.name },
                coverUrl = track.al.picUrl,
                isVip = track.fee == 1
            ),
            isActive = currentTrackId == track.id.toString(),
            isPlaying = isPlaying,
            onClick = { onPlaySong(track) },
            onArtistClick = { track.ar.firstOrNull()?.id?.let(onArtistClick) },
            trailingSlot = {
                IconButton(
                    onClick = { onAddRecommendSong(track) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加歌曲到歌单",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .padding(MelodiaSpacing.xxs)
                    )
                }
            }
        )
    }
}

// 推荐板块头部
@Composable
private fun RecommendationHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "推荐歌曲",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRefresh)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新推荐",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(MelodiaSpacing.xs))
            Text(
                text = "刷新",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

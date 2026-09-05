package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.components.MelodiaIconButton
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// ────────────────────────────────────────────────────────────────────────────
// 歌曲列表（支持搜索过滤）
// ────────────────────────────────────────────────────────────────────────────
fun LazyListScope.playlistTrackItems(
    tracks: List<Track>,
    searchQuery: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    likedSongIds: Set<Long> = emptySet(),
    isLoggedIn: Boolean = false,
    onPlaySong: (Track) -> Unit,
    onLikeClick: (Long) -> Unit = {},
    onOpenCollectSheet: (Long) -> Unit = {},
    onMoreClick: (Track) -> Unit
) {
    val filtered = if (searchQuery.isBlank()) tracks
                   else tracks.filter {
                       it.name.contains(searchQuery, true) ||
                       it.ar.any { a -> a.name.contains(searchQuery, true) }
                   }
    items(filtered, key = { it.id }) { track ->
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
            trailingSlot = {
                if (isLoggedIn && track.id in likedSongIds) {
                    MelodiaIconButton(
                        onClick = {
                            onOpenCollectSheet(track.id)
                            onLikeClick(track.id)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "已收藏歌曲",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                MelodiaIconButton(
                    onClick = { onMoreClick(track) },
                    modifier = Modifier.size(32.dp).padding(end = MelodiaSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}

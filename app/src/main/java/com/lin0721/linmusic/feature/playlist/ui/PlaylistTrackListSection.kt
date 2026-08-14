package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onPlaySong: (Track) -> Unit,
    onArtistClick: (Long) -> Unit,
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
            onClick = { onPlaySong(track) },
            onArtistClick = { track.ar.firstOrNull()?.id?.let(onArtistClick) },
            trailingSlot = {
                IconButton(
                    onClick = { onMoreClick(track) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(MelodiaSpacing.sm))
            }
        )
    }
}

package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// 音乐 Tab: 显示当前歌手的全部热门歌曲
fun LazyListScope.artistMusicTab(
    topSongs: List<Track>,
    likedSongIds: Set<Long>,
    currentTrackId: String?,
    isLoggedIn: Boolean,
    onPlaySong: (Track) -> Unit,
    onLikeClick: (Long) -> Unit,
    onOpenCollectSheet: (Long) -> Unit,
    onRequireLogin: () -> Unit
) {
    if (topSongs.isEmpty()) {
        item(key = "empty_songs") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    } else {
        itemsIndexed(topSongs, key = { _, track -> track.id }) { index, track ->
            val isActive = track.id.toString() == currentTrackId
            SongRow(
                data = SongRowData(
                    id = track.id,
                    title = track.name,
                    artist = track.ar.joinToString(" • ") { it.name },
                    coverUrl = track.al.picUrl,
                    isVip = track.fee == 1
                ),
                isActive = isActive,
                index = index + 1,
                onClick = { onPlaySong(track) },
                trailingSlot = {
                    val isLiked = track.id in likedSongIds
                    IconButton(
                        onClick = {
                            if (!isLoggedIn) {
                                onRequireLogin()
                            } else {
                                onOpenCollectSheet(track.id)
                                onLikeClick(track.id)
                            }
                        },
                        modifier = Modifier.size(32.dp).padding(end = MelodiaSpacing.xs)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "喜欢/收藏歌曲",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    }
}

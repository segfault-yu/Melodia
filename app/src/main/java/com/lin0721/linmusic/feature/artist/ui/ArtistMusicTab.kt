package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.FilterChipsRow
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

private val MUSIC_SUB_TABS = listOf("热门50首", "全部歌曲")

// 音乐 Tab: 二级切换「热门50首」（策展）与「全部歌曲」（滚动分页）
fun LazyListScope.artistMusicTab(
    hotSongs: List<Track>,
    allSongs: List<Track>,
    musicSubTab: Int,
    onMusicSubTabSelected: (Int) -> Unit,
    allSongsLoadingMore: Boolean,
    likedSongIds: Set<Long>,
    currentTrackId: String?,
    isLoggedIn: Boolean,
    onPlaySong: (Track) -> Unit,
    onLikeClick: (Long) -> Unit,
    onOpenCollectSheet: (Long) -> Unit,
    onOpenMoreOptions: (Track) -> Unit,
    onRequireLogin: () -> Unit
) {
    item(key = "music_sub_tab") {
        FilterChipsRow(
            items = MUSIC_SUB_TABS,
            selectedIndex = musicSubTab,
            onSelected = onMusicSubTabSelected
        )
    }

    val songs = if (musicSubTab == 0) hotSongs else allSongs

    if (songs.isEmpty() && !(musicSubTab == 1 && allSongsLoadingMore)) {
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
        itemsIndexed(songs, key = { _, track -> "${musicSubTab}_${track.id}" }) { index, track ->
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "喜欢/收藏歌曲",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onOpenMoreOptions(track) },
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

        if (musicSubTab == 1 && allSongsLoadingMore) {
            item(key = "all_songs_loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MelodiaSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

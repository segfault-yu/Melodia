package com.lin0721.linmusic.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.GradientStart
import com.lin0721.linmusic.feature.home.ui.ErrorContent
import com.lin0721.linmusic.feature.home.ui.FilterPills
import com.lin0721.linmusic.feature.home.ui.LoadingIndicator
import com.lin0721.linmusic.feature.home.ui.TopGreetingBar
import com.lin0721.linmusic.feature.music.domain.StyleArtistItem
import com.lin0721.linmusic.feature.music.domain.StylePlaylistItem

// 「音乐」tab 骨架：顶栏与「全部」共用，胶囊以下是曲风体系。
// 与「全部」并列而非塞进同一个 Content，是为了让第三个 tab 后续能独立加进来。
@Composable
fun MusicContent(
    uiState: MusicUiState,
    userProfile: UserProfile?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onStyleSelect: (StyleSelection) -> Unit,
    onChildStyleSelect: (Long?) -> Unit,
    onPlaylistClick: (StylePlaylistItem) -> Unit,
    onArtistClick: (StyleArtistItem) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onPlayFavourite: () -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 180.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GradientStart, BackgroundDark)))
                    .statusBarsPadding()
            ) {
                TopGreetingBar(
                    userProfile = userProfile,
                    onLoginClick = onAvatarClick,
                    onSearchClick = onSearchClick
                )
                FilterPills(selectedIndex = selectedTab, onSelected = onTabSelected)
            }
        }

        when (uiState) {
            is MusicUiState.Loading -> item { LoadingIndicator() }

            is MusicUiState.Error -> item {
                ErrorContent(message = uiState.message, onRetry = onRetry)
            }

            is MusicUiState.Success -> {
                val data = uiState.data

                item {
                    MusicStyleChips(
                        styles = data.styles,
                        selection = data.selection,
                        showPreference = data.hasPreference,
                        onSelect = onStyleSelect
                    )
                }

                // 偏好页展示画像与占比，具体曲风页展示头图与二级筛选
                if (data.selection is StyleSelection.Preference) {
                    data.content?.head?.portrait?.let { portrait ->
                        item {
                            MusicPortraitCard(
                                portrait = portrait,
                                accent = data.preferences.firstOrNull()?.colorHex.toStyleColor()
                            )
                        }
                    }
                    item { MusicSectionTitle("偏好占比") }
                    item { MusicPreferenceBars(data.preferences) }
                } else {
                    data.content?.head?.let { head ->
                        item { MusicStyleHeader(head = head, onPlay = { onPlaySongAt(0) }) }
                    }
                    data.selectedStyle?.children?.takeIf { it.isNotEmpty() }?.let { children ->
                        item {
                            MusicSubStyleChips(
                                children = children,
                                selectedChildId = data.selectedChildId,
                                onSelect = onChildStyleSelect
                            )
                        }
                    }
                }

                if (data.isContentLoading) {
                    item { MusicSectionLoading() }
                } else {
                    val content = data.content

                    content?.head?.favouriteSong?.let { track ->
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MusicSectionTitle("你的最爱")
                                MusicFavouriteSongCard(track = track, onClick = onPlayFavourite)
                            }
                        }
                    }

                    content?.playlists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                        item { MusicSectionTitle("热门歌单") }
                        item { MusicPlaylistRow(playlists = playlists, onClick = onPlaylistClick) }
                    }

                    content?.songs?.takeIf { it.isNotEmpty() }?.let { songs ->
                        item { MusicSectionTitle("必听单曲") }
                        item { MusicSongList(songs = songs, onPlayAt = onPlaySongAt) }
                    }

                    content?.artists?.takeIf { it.isNotEmpty() }?.let { artists ->
                        item { MusicSectionTitle("代表歌手") }
                        item { MusicArtistRow(artists = artists, onClick = onArtistClick) }
                    }
                }
            }
        }
    }
}

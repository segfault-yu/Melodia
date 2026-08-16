package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectSheet
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.ui.theme.CoverPlaceholderDark

// 歌手详情页骨架：持有折叠进度与弹层状态，按 Tab 装配各内容区块
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistContent(
    artist: ArtistDetailInfo,
    isFollowed: Boolean,
    fansCount: Long,
    topSongs: List<Track>,
    albums: List<ArtistAlbum>,
    similarArtists: List<ArtistInfo>,
    likedSongIds: Set<Long>,
    blockedArtistIds: Set<Long>,
    currentTrackId: String?,
    collectState: PlaylistCollectState,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onFollowClick: () -> Unit,
    onBlockClick: () -> Unit,
    onPlaySong: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveCollection: (Long, List<PlaylistCollectItem>) -> Unit,
    onSaveNewCollection: (String, Long) -> Unit,
    onRequireLogin: () -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var showMoreMenuSheet by remember { mutableStateOf(false) }

    var dominantColor by remember { mutableStateOf(CoverPlaceholderDark) }

    // 背景折叠临界点
    val collapseThresholdPx = with(density) { 320.dp.toPx() }
    val progress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
        }
    }

    var collectSongId by remember { mutableStateOf<Long?>(null) }
    var showBioDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {

        ArtistBackdrop(
            artist = artist,
            progress = progress,
            collapseThresholdPx = collapseThresholdPx,
            dominantColor = dominantColor,
            onDominantColorChange = { dominantColor = it }
        )

        // 滚动视口列表
        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 180.dp)
            ) {
                item(key = "header_placeholder") {
                    ArtistNameSection(artist = artist)
                }

                item(key = "action_bar") {
                    ArtistActionBar(
                        artist = artist,
                        fansCount = fansCount,
                        isFollowed = isFollowed,
                        onFollowClick = onFollowClick,
                        onMoreClick = { showMoreMenuSheet = true },
                        onPlayAll = onPlayAll
                    )
                }

                item(key = "tab_bar") {
                    ArtistTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                // 根据选中的 Tab 呈现对应内容
                when (selectedTab) {
                    0 -> artistMusicTab(
                        topSongs = topSongs,
                        likedSongIds = likedSongIds,
                        currentTrackId = currentTrackId,
                        isLoggedIn = isLoggedIn,
                        onPlaySong = onPlaySong,
                        onLikeClick = onLikeClick,
                        onOpenCollectSheet = { collectSongId = it },
                        onRequireLogin = onRequireLogin
                    )
                    1 -> artistAlbumTab(
                        albums = albums,
                        onAlbumClick = onAlbumClick
                    )
                    2 -> artistMvTab()
                    3 -> artistAboutTab(
                        artist = artist,
                        similarArtists = similarArtists,
                        onShowBioDialog = { showBioDialog = true },
                        onArtistClick = onArtistClick
                    )
                }
            }
        }

        ArtistTopBarOverlay(
            artistName = artist.name,
            progress = progress,
            dominantColor = dominantColor,
            onBack = onBack
        )

        // 详情 Bottom Sheet (批量收藏)
        if (collectSongId != null) {
            PlaylistCollectSheet(
                songId = collectSongId!!,
                collectState = collectState,
                onDismiss = { collectSongId = null },
                onSaveCollection = onSaveCollection,
                onSaveNewCollection = onSaveNewCollection,
                sheetShape = BottomSheetShape,
                itemCornerRadius = 6.dp,
                confirmButtonShape = RoundedCornerShape(12.dp)
            )
        }

        // 歌手简介 Dialog
        if (showBioDialog) {
            ArtistBioDialog(
                artist = artist,
                onDismiss = { showBioDialog = false }
            )
        }

        // 更多操作 Bottom Sheet
        if (showMoreMenuSheet) {
            ArtistMoreMenuSheet(
                isBlocked = blockedArtistIds.contains(artist.id),
                onBlockClick = onBlockClick,
                onDismiss = { showMoreMenuSheet = false }
            )
        }
    }
}

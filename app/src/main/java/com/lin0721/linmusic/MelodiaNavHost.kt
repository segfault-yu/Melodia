package com.lin0721.linmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.lin0721.linmusic.feature.home.ui.HomeScreen
import com.lin0721.linmusic.feature.home.ui.HomeViewModel

// ────────────────────────────────────────────────────────────────────────────
// 六个主屏幕之间的切换动画与路由分发
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun MelodiaNavHost(
    currentScreen: Screen,
    homeViewModel: HomeViewModel,
    activePlaylistId: Long?,
    activePlaylistIsAlbum: Boolean,
    activeArtistId: Long?,
    activeRadioId: Long?,
    activeMvId: Long?,
    activeMvName: String,
    activePlaylistCategory: String?,
    homeTab: Int,
    searchAutoFocus: Boolean,
    onOpenSidebar: () -> Unit,
    onLoginScreenVisibilityChanged: (Boolean) -> Unit,
    onNavigateToPlaylist: (id: Long, isAlbum: Boolean) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToRadio: (Long) -> Unit,
    onNavigateToMv: (Long, String) -> Unit,
    onMvFullscreenChanged: (Boolean) -> Unit,
    onNavigateToPlaylistCategory: (String) -> Unit,
    onHomeTabSelected: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onBack: () -> Unit
) {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val forward = targetState != Screen.Home
            val offsetY = 40
            if (forward) {
                (fadeIn(tween(300, delayMillis = 100, easing = FastOutSlowInEasing))
                        + slideInVertically(tween(300, delayMillis = 100, easing = FastOutSlowInEasing)) { offsetY })
                    .togetherWith(
                        fadeOut(tween(200, easing = FastOutSlowInEasing))
                                + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -offsetY }
                    )
            } else {
                (fadeIn(tween(300, delayMillis = 100, easing = FastOutSlowInEasing))
                        + slideInVertically(tween(300, delayMillis = 100, easing = FastOutSlowInEasing)) { -offsetY })
                    .togetherWith(
                        fadeOut(tween(200, easing = FastOutSlowInEasing))
                                + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { offsetY }
                    )
            }.using(SizeTransform(clip = false))
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.Home -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    selectedTab = homeTab,
                    onTabSelected = onHomeTabSelected,
                    onPlaylistClick = onNavigateToPlaylist,
                    onArtistClick = onNavigateToArtist,
                    onRadioClick = onNavigateToRadio,
                    onSearchClick = onNavigateToSearch,
                    onOpenSidebar = onOpenSidebar,
                    onLoginScreenVisibilityChanged = onLoginScreenVisibilityChanged
                )
            }
            Screen.Playlist -> {
                activePlaylistId?.let { id ->
                    com.lin0721.linmusic.feature.playlist.ui.PlaylistScreen(
                        playlistId = id,
                        isAlbum = activePlaylistIsAlbum,
                        onBack = onBack,
                        onArtistClick = onNavigateToArtist,
                        onAlbumClick = { albumId -> onNavigateToPlaylist(albumId, true) }
                    )
                }
            }
            Screen.Search -> {
                com.lin0721.linmusic.feature.search.ui.SearchScreen(
                    autoFocus = searchAutoFocus,
                    onOpenSidebar = onOpenSidebar,
                    onPlaylistClick = onNavigateToPlaylist,
                    onArtistClick = onNavigateToArtist,
                    onPlaylistCategoryClick = onNavigateToPlaylistCategory
                )
            }
            Screen.Library -> {
                com.lin0721.linmusic.feature.library.ui.LibraryScreen(
                    onPlaylistClick = { id -> onNavigateToPlaylist(id, false) },
                    onArtistClick = onNavigateToArtist,
                    onBack = onBack,
                    onOpenSidebar = onOpenSidebar,
                    onLoginScreenVisibilityChanged = onLoginScreenVisibilityChanged
                )
            }
            Screen.Settings -> {
                com.lin0721.linmusic.feature.settings.ui.SettingsScreen(
                    onBack = onBack
                )
            }
            Screen.Radio -> {
                activeRadioId?.let { id ->
                    com.lin0721.linmusic.feature.podcast.ui.RadioDetailScreen(
                        radioId = id,
                        onBack = onBack
                    )
                }
            }
            Screen.Artist -> {
                activeArtistId?.let { id ->
                    com.lin0721.linmusic.feature.artist.ui.ArtistScreen(
                        artistId = id,
                        onBack = onBack,
                        onArtistClick = onNavigateToArtist,
                        onPlaylistClick = { playlistId -> onNavigateToPlaylist(playlistId, false) },
                        onAlbumClick = { albumId -> onNavigateToPlaylist(albumId, true) },
                        onMvClick = onNavigateToMv
                    )
                }
            }
            Screen.MvPlayer -> {
                activeMvId?.let { id ->
                    com.lin0721.linmusic.feature.artist.ui.ArtistMvPlayerScreen(
                        mvId = id,
                        mvName = activeMvName,
                        onBack = onBack,
                        onArtistClick = onNavigateToArtist,
                        onMvClick = onNavigateToMv,
                        onFullscreenChanged = onMvFullscreenChanged
                    )
                }
            }
            Screen.PlaylistCategory -> {
                activePlaylistCategory?.let { category ->
                    com.lin0721.linmusic.feature.search.ui.PlaylistCategoryScreen(
                        category = category,
                        onBack = onBack,
                        onPlaylistClick = { id -> onNavigateToPlaylist(id, false) }
                    )
                }
            }
        }
    }
}

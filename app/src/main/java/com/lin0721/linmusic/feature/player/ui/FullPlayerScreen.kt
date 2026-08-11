package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.lin0721.linmusic.feature.artist.data.ArtistAlbum
import com.lin0721.linmusic.feature.artist.data.ArtistDetailInfo
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.comment.data.CommentItem
import com.lin0721.linmusic.feature.comment.ui.CommentsBottomSheet
import com.lin0721.linmusic.feature.comment.ui.CommentsPreviewCard
import com.lin0721.linmusic.feature.artist.domain.ArtistInfo
import com.lin0721.linmusic.feature.player.domain.LyricLine
import com.lin0721.linmusic.feature.player.domain.SongWikiData
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.ColorPalette
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.PaletteMemoryCache
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import org.koin.androidx.compose.koinViewModel
import com.lin0721.linmusic.core.player.PlayMode
import androidx.activity.compose.BackHandler
import com.lin0721.linmusic.core.ui.components.ToastManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    isPlayerOpen: Boolean,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onDragClose: (Float, Float) -> Unit = { _, _ -> }
) {
    if (currentTrack == null) return

    val viewModel: PlayerViewModel = koinViewModel()
    val songDetailState by viewModel.songDetailState.collectAsStateWithLifecycle()
    val lyrics = songDetailState.lyrics
    val isLyricsLoading = songDetailState.isLyricsLoading
    val songDetail = songDetailState.songDetail
    val similarArtists = songDetailState.similarArtists
    val isSimilarArtistsLoading = songDetailState.isSimilarArtistsLoading
    val artistDetail = songDetailState.artistDetail
    val artistFansCount = songDetailState.artistFansCount
    val artistAlbums = songDetailState.artistAlbums
    val isLiked = songDetailState.isLiked
    val songWiki = songDetailState.songWiki
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsStateWithLifecycle()
    val playContext by viewModel.playerManager.playContext.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val activeQuality by viewModel.activeQuality.collectAsStateWithLifecycle()
    val playMode by viewModel.playerManager.playMode.collectAsStateWithLifecycle()
    val queue by viewModel.playerManager.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.playerManager.currentIndex.collectAsStateWithLifecycle()
    var showQueueSheet by remember { mutableStateOf(false) }
    var isLyricsFullScreen by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            com.lin0721.linmusic.core.ui.components.ToastManager.showToast(message)
        }
    }

    BackHandler(enabled = showQueueSheet) {
        showQueueSheet = false
    }

    BackHandler(enabled = isLyricsFullScreen) {
        isLyricsFullScreen = false
    }

    BackHandler(enabled = showMoreOptionsSheet) {
        showMoreOptionsSheet = false
    }

    BackHandler(enabled = showTimerSheet) {
        showTimerSheet = false
    }

    BackHandler(enabled = showCommentsSheet) {
        showCommentsSheet = false
    }

    DisposableEffect(isLyricsFullScreen, isPlaying) {
        if (isLyricsFullScreen && isPlaying) {
            viewModel.playerManager.setPositionUpdateInterval(50L)
        } else {
            viewModel.playerManager.setPositionUpdateInterval(1000L)
        }
        onDispose {
            viewModel.playerManager.setPositionUpdateInterval(1000L)
        }
    }

    var colorPalette by remember(currentTrack.mediaId) {
        mutableStateOf(
            PaletteMemoryCache.get(currentTrack.mediaId) ?: ColorPalette(Color(0xFF333333), Color(0xFF222222))
        )
    }
    val animatedDominant by animateColorAsState(
        targetValue = colorPalette.dominant,
        animationSpec = tween(800),
        label = "bg_dominant"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colorPalette.secondary,
        animationSpec = tween(800),
        label = "bg_secondary"
    )

    val tintedCardColor = animatedDominant.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface)

    val gradientStart = remember(animatedDominant) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (animatedDominant.red * 255).toInt(),
            (animatedDominant.green * 255).toInt(),
            (animatedDominant.blue * 255).toInt(),
            hsv
        )
        if (hsv[1] < 0.05f) {
            hsv[1] = 0f
            hsv[2] = 0.15f
        } else {
            hsv[1] = (hsv[1] * 0.6f).coerceIn(0.35f, 1f)
            hsv[2] = 0.92f
        }
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    val gradientEnd = remember(animatedDominant) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (animatedDominant.red * 255).toInt(),
            (animatedDominant.green * 255).toInt(),
            (animatedDominant.blue * 255).toInt(),
            hsv
        )
        if (hsv[1] < 0.05f) {
            hsv[1] = 0f
            hsv[2] = 0.05f
        } else {
            hsv[1] = (hsv[1] + 0.35f).coerceIn(0.75f, 1f)
            hsv[2] = 0.3f
        }
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val accentColor = remember(animatedDominant) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (animatedDominant.red * 255).toInt(),
            (animatedDominant.green * 255).toInt(),
            (animatedDominant.blue * 255).toInt(),
            hsv
        )
        if (hsv[1] < 0.05f) {
            hsv[1] = 0f
            hsv[2] = 0.3f
        } else {
            hsv[1] = 1.0f
            hsv[2] = 0.75f
        }
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    val lyricsHighlight = lerp(start = animatedDominant, stop = Color.White, fraction = 0.85f)

    val infiniteTransition = rememberInfiniteTransition(label = "bg_breathe")
    val gradientEndY by infiniteTransition.animateFloat(
        initialValue = 2400f,
        targetValue = 3000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_end"
    )

    val title = currentTrack.mediaMetadata.title?.toString() ?: ""
    val artist = currentTrack.mediaMetadata.artist?.toString() ?: ""
    val coverUrl = currentTrack.mediaMetadata.artworkUri?.toString()
        ?.replace("?param=300y300", "") ?: ""

    val listState = rememberLazyListState()
    val showTitleInBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val hazeState = remember { HazeState() }

    val coverScale by remember {
        derivedStateOf {
            val coverItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "cover" }
            if (coverItem != null) {
                val fraction = (-coverItem.offset.toFloat() / coverItem.size).coerceIn(0f, 1f)
                1f - fraction * 0.15f
            } else 0.85f
        }
    }
    var coverHeight by remember { mutableStateOf(1000f) }
    val backgroundTranslationY by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val coverItem = visibleItems.firstOrNull { it.key == "cover" }
            if (coverItem != null) {
                coverHeight = coverItem.size.toFloat()
                coverItem.offset.toFloat()
            } else {
                val firstIndex = listState.firstVisibleItemIndex
                val firstOffset = listState.firstVisibleItemScrollOffset
                val estimatedSubsequentScroll = (firstIndex - 1) * 250f + firstOffset
                -(coverHeight + estimatedSubsequentScroll)
            }
        }
    }

    var offsetY by remember { mutableStateOf(0f) }
    var isScrollGestureActive by remember { mutableStateOf(false) }
    var isGestureStartedAtTop by remember { mutableStateOf(true) }

    LaunchedEffect(isPlayerOpen) {
        if (isPlayerOpen) {
            offsetY = 0f
            isScrollGestureActive = false
            isGestureStartedAtTop = true
            isLyricsFullScreen = false
        }
    }

    var screenHeightPx by remember { mutableStateOf(0f) }
    val topCornerRadius by remember {
        derivedStateOf {
            if (offsetY > 0f) 24.dp else 0.dp
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var dragReleaseJob by remember { mutableStateOf<Job?>(null) }

    fun handleDragRelease(velocity: Float = 0f) {
        dragReleaseJob?.cancel()
        dragReleaseJob = coroutineScope.launch {
            val shouldClose = if (isGestureStartedAtTop) {
                offsetY > screenHeightPx * 0.20f || velocity > 1000f
            } else {
                offsetY > screenHeightPx * 0.20f
            }

            if (offsetY > 0f && shouldClose) {
                val finalOffset = offsetY
                offsetY = 0f
                onDragClose(finalOffset, velocity)
            } else {
                animate(
                    initialValue = offsetY,
                    targetValue = 0f,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ ->
                    offsetY = value.coerceAtLeast(0f)
                }
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (!isScrollGestureActive) {
                        isScrollGestureActive = true
                        isGestureStartedAtTop = listState.firstVisibleItemIndex == 0
                    }
                }

                return if (offsetY > 0f && available.y < 0f) {
                    val damping = if (isGestureStartedAtTop) 1.0f else 0.3f
                    val delta = available.y * damping
                    val consumed = delta.coerceAtLeast(-offsetY)
                    offsetY += consumed
                    Offset(0f, consumed / damping)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (!isScrollGestureActive) {
                        isScrollGestureActive = true
                        isGestureStartedAtTop = listState.firstVisibleItemIndex == 0
                    }
                }

                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                return if (available.y > 0f && isAtTop && source == NestedScrollSource.UserInput) {
                    val damping = if (isGestureStartedAtTop) 1.0f else 0.3f
                    offsetY += available.y * damping
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                isScrollGestureActive = false
                return if (offsetY > 0f) {
                    handleDragRelease(velocity = available.y)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isScrollGestureActive = false
                return if (offsetY > 0f) {
                    handleDragRelease(velocity = available.y)
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeightPx = it.height.toFloat() }
            .nestedScroll(nestedScrollConnection)
            .graphicsLayer {
                translationY = offsetY
            }
            .clip(RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1200.dp)
                .graphicsLayer {
                    translationY = backgroundTranslationY
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedDominant,
                            animatedDominant.copy(alpha = 0.75f),
                            animatedDominant.copy(alpha = 0.5f),
                            animatedDominant.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = gradientEndY
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 80.dp
            )
        ) {
            item(key = "cover") {
                CoverArt(
                    coverUrl = coverUrl,
                    title = title,
                    playContext = playContext,
                    onClose = onClose,
                    onPaletteExtracted = {
                        colorPalette = it
                        PaletteMemoryCache.put(currentTrack.mediaId, it)
                    },
                    onMoreClick = { showMoreOptionsSheet = true },
                    modifier = Modifier.graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                    }
                )
            }

            item(key = "song_info") {
                SongInfo(
                    title = title,
                    artist = artist,
                    isLiked = isLiked,
                    onToggleLike = viewModel::toggleLike,
                    onArtistClick = {
                        songDetail?.ar?.firstOrNull()?.id?.let { id ->
                            onClose()
                            onArtistClick(id)
                        }
                    }
                )
            }

            item(key = "mini_lyric") {
                MiniLyricLine(
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    lyricsHighlight = lyricsHighlight,
                    isPlaying = isPlaying
                )
            }

            item(key = "progress") {
                val displayDuration = if (duration > 0L) duration else (songDetail?.dt ?: 0L)
                ProgressSection(
                    currentPositionProvider = currentPositionProvider,
                    duration = displayDuration,
                    onSeek = onSeek
                )
            }

            item(key = "controls") {
                PlaybackControls(
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlay,
                    onPlayNext = viewModel.playerManager::playNext,
                    onPlayPrevious = viewModel.playerManager::playPrevious,
                    onToggleShuffle = viewModel.playerManager::toggleShuffle,
                    onToggleRepeat = viewModel.playerManager::toggleRepeat,
                    playMode = playMode,
                    isRoaming = playContext == "similar_roaming",
                    onDisableRoaming = { viewModel.playerManager.disableRoaming() }
                )
            }

            item(key = "actions") {
                ActionButtons(
                    onTimerClick = { showTimerSheet = true },
                    onQueueClick = { showQueueSheet = true },
                    onInsertSimilarClick = {
                        val songId = currentTrack.mediaId?.toLongOrNull()
                        if (songId != null && songId != -1L) {
                            viewModel.insertSimilarSongs(songId)
                        }
                    }
                )
            }

            val isPureMusic = lyrics.size == 1 && lyrics[0].text == "纯音乐"
            if (isLyricsLoading || (lyrics.isNotEmpty() && !isPureMusic)) {
                item(key = "lyrics") {
                    LyricsCard(
                        lyrics = lyrics,
                        currentIndex = currentLyricIndex,
                        isLoading = isLyricsLoading,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        accentColor = accentColor,
                        highlightColor = lyricsHighlight,
                        onOpenFullScreen = { isLyricsFullScreen = true }
                    )
                }
            }

            item(key = "comments_preview") {
                CommentsPreviewCard(
                    commentsState = commentsState,
                    cardColor = MaterialTheme.colorScheme.surface,
                    onClick = { showCommentsSheet = true },
                    onRetry = viewModel::retryComments
                )
            }

            item(key = "song_detail") {
                SongDetailCard(songWiki = songWiki, songDetail = songDetail, cardColor = MaterialTheme.colorScheme.surface)
            }

            item(key = "about_artist") {
                val isArtistFollowed = songDetailState.isArtistFollowed
                AboutArtistCard(
                    artistDetail = artistDetail,
                    fansCount = artistFansCount,
                    isFollowed = isArtistFollowed,
                    onFollowClick = { viewModel.toggleArtistFollow() },
                    cardColor = MaterialTheme.colorScheme.surface,
                    onClick = {
                        artistDetail?.id?.let { id ->
                            onArtistClick(id)
                        }
                    }
                )
            }

            item(key = "similar_artists") {
                SimilarArtistsCard(
                    artists = similarArtists,
                    isLoading = isSimilarArtistsLoading,
                    cardColor = MaterialTheme.colorScheme.surface,
                    onArtistClick = onArtistClick
                )
            }

            item(key = "artist_albums") {
                ArtistAlbumsCard(
                    albums = artistAlbums,
                    artistName = artistDetail?.name,
                    cardColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        TopBar(
            onClose = onClose,
            title = title,
            artist = artist,
            showTitle = showTitleInBar,
            isPlaying = isPlaying,
            onTogglePlay = onTogglePlay,
            isLiked = isLiked,
            onToggleLike = viewModel::toggleLike,
            backgroundColor = animatedDominant,
            onArtistClick = {
                songDetail?.ar?.firstOrNull()?.id?.let { id ->
                    onClose()
                    onArtistClick(id)
                }
            },
            modifier = Modifier.draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    offsetY = (offsetY + delta).coerceAtLeast(0f)
                },
                onDragStarted = {
                    isGestureStartedAtTop = true
                },
                onDragStopped = { velocity ->
                    handleDragRelease(velocity = velocity)
                }
            )
        )

        if (showQueueSheet) {
            PlayQueueSheet(
                queue = queue,
                currentIndex = currentQueueIndex,
                playMode = playMode,
                playContext = playContext,
                isPlaying = isPlaying,
                onPlayAtIndex = { viewModel.playerManager.playAtIndex(it) },
                onRemoveAtIndex = { viewModel.playerManager.removeFromQueue(it) },
                onMoveItem = { from, to -> viewModel.playerManager.moveInQueue(from, to) },
                onToggleShuffle = viewModel.playerManager::toggleShuffle,
                onClearQueue = {
                    viewModel.clearQueue()
                    showQueueSheet = false
                },
                onDisableRoaming = { viewModel.playerManager.disableRoaming() },
                onDismiss = { showQueueSheet = false }
            )
        }

        AnimatedVisibility(
            visible = isLyricsFullScreen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            FullScreenLyricsView(
                lyrics = lyrics,
                currentIndex = currentLyricIndex,
                isLoading = isLyricsLoading,
                title = title,
                artist = artist,
                gradientStart = gradientStart,
                gradientEnd = gradientEnd,
                accentColor = accentColor,
                highlightColor = lyricsHighlight,
                onSeek = { timeMs ->
                    viewModel.seekToTime(timeMs)
                },
                hazeState = hazeState,
                onClose = { isLyricsFullScreen = false },
                isPlaying = isPlaying,
                currentPositionProvider = currentPositionProvider,
                duration = duration,
                onTogglePlay = onTogglePlay,
                onPlayNext = viewModel.playerManager::playNext,
                onPlayPrevious = viewModel.playerManager::playPrevious,
                playMode = playMode,
                onToggleShuffle = viewModel.playerManager::toggleShuffle,
                onToggleRepeat = viewModel.playerManager::toggleRepeat,
                onMoreClick = { showMoreOptionsSheet = true }
            )
        }

        if (showMoreOptionsSheet) {
            val albumName = songDetail?.al?.name ?: songWiki?.album ?: "未知专辑"
            SongMoreOptionsSheet(
                title = title,
                artist = artist,
                coverUrl = coverUrl,
                albumName = albumName,
                isLiked = isLiked,
                sleepTimerRemaining = sleepTimerRemaining,
                currentQuality = activeQuality,
                onToggleLike = viewModel::toggleLike,

                onAlbumClick = {
                    val albumId = songDetail?.al?.id ?: 0L
                    if (albumId > 0L) {
                        onClose()
                        onAlbumClick(albumId)
                    } else {
                        ToastManager.showToast("未找到专辑信息")
                    }
                },
                onArtistClick = {
                    val artistId = songDetail?.ar?.firstOrNull()?.id ?: 0L
                    if (artistId > 0L) {
                        onClose()
                        onArtistClick(artistId)
                    } else {
                        ToastManager.showToast("未找到歌手信息")
                    }
                },
                onShowTimerClick = {
                    showTimerSheet = true
                },
                onQualitySelected = viewModel::updateQuality,
                onStartSimilarRoaming = {
                    val songId = currentTrack.mediaId.toLongOrNull() ?: 0L
                    viewModel.startSimilarSongsRoaming(songId, title, artist, coverUrl)
                },
                onInsertSimilarSongs = {
                    val songId = currentTrack.mediaId.toLongOrNull() ?: 0L
                    viewModel.insertSimilarSongs(songId)
                },
                onDismiss = { showMoreOptionsSheet = false }
            )
        }

        if (showTimerSheet) {
            SleepTimerSheet(
                sleepTimerRemaining = sleepTimerRemaining,
                onSetTimer = { minutes ->
                    viewModel.setSleepTimer(minutes)
                    showTimerSheet = false
                },
                onDismiss = { showTimerSheet = false }
            )
        }

        if (showCommentsSheet) {
            CommentsBottomSheet(
                commentsState = commentsState,
                onLikeComment = viewModel::likeComment,
                onDismiss = { showCommentsSheet = false },
                onRetry = { viewModel.retryComments() }
            )
        }
    }
}

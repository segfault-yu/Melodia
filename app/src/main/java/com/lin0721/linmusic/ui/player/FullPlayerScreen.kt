package com.lin0721.linmusic.ui.player

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
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
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.remote.api.CommentItem
import com.lin0721.linmusic.data.repository.ArtistInfo
import com.lin0721.linmusic.data.repository.LyricLine
import com.lin0721.linmusic.data.repository.SongWikiData
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.ColorPalette
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.SurfaceLight
import com.lin0721.linmusic.ui.theme.TextGray
import com.lin0721.linmusic.ui.theme.extractColorPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import org.koin.androidx.compose.koinViewModel
import com.lin0721.linmusic.player.PlayMode
import androidx.activity.compose.BackHandler

@Composable
fun FullPlayerScreen(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    isPlayerOpen: Boolean
) {
    if (currentTrack == null) return

    val viewModel: PlayerViewModel = koinViewModel()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsStateWithLifecycle()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsStateWithLifecycle()
    val songDetail by viewModel.songDetail.collectAsStateWithLifecycle()
    val similarArtists by viewModel.similarArtists.collectAsStateWithLifecycle()
    val isSimilarArtistsLoading by viewModel.isSimilarArtistsLoading.collectAsStateWithLifecycle()
    val artistDetail by viewModel.artistDetail.collectAsStateWithLifecycle()
    // 歌手粉丝数量 (用于每月听众数)
    val artistFansCount by viewModel.artistFansCount.collectAsStateWithLifecycle()
    val artistAlbums by viewModel.artistAlbums.collectAsStateWithLifecycle()
    val playContext by viewModel.playerManager.playContext.collectAsStateWithLifecycle()
    val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val playMode by viewModel.playerManager.playMode.collectAsStateWithLifecycle()
    val queue by viewModel.playerManager.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.playerManager.currentIndex.collectAsStateWithLifecycle()
    val songWiki by viewModel.songWiki.collectAsStateWithLifecycle()
    var showQueueSheet by remember { mutableStateOf(false) }

    // 当播放队列展开时，拦截系统返回键仅收起队列，避免直接关闭整个播放界面
    BackHandler(enabled = showQueueSheet) {
        showQueueSheet = false
    }

    var colorPalette by remember { mutableStateOf(ColorPalette(Color(0xFF333333), Color(0xFF222222))) }
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

    val tintedCardColor = animatedDominant.copy(alpha = 0.08f).compositeOver(SurfaceDark)

    fun Color.toOpaqueHsv(minSat: Float, valRange: ClosedFloatingPointRange<Float>): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), hsv
        )
        hsv[1] = hsv[1].coerceAtLeast(minSat)
        hsv[2] = hsv[2].coerceIn(valRange.start, valRange.endInclusive)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    val gradientStart = animatedSecondary.toOpaqueHsv(minSat = 0.8f, valRange = 0.55f..0.7f)
    val gradientEnd = animatedSecondary.toOpaqueHsv(minSat = 0.9f, valRange = 0.35f..0.45f)
    val lyricsCardBrush = Brush.linearGradient(colors = listOf(gradientEnd, gradientStart))

    val lyricsHighlight = lerp(start = animatedDominant, stop = Color.White, fraction = 0.85f)

    val infiniteTransition = rememberInfiniteTransition(label = "bg_breathe")
    val gradientEndY by infiniteTransition.animateFloat(
        initialValue = 2400f, // 调大结束端点像素值，使渐变光效向下方大幅延伸
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
    var coverHeight by remember { mutableStateOf(1000f) } // 缓存封面的高度像素值（提供默认备用值）
    val backgroundTranslationY by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val coverItem = visibleItems.firstOrNull { it.key == "cover" }
            if (coverItem != null) {
                coverHeight = coverItem.size.toFloat()
                coverItem.offset.toFloat()
            } else {
                // 当封面滚动到屏幕外被回收后，依据列表首项索引及偏移量进行数学平滑估算，
                // 确保光效背景跟随滚动继续向上平移，绝不产生突然跳变到 -2000f 的断层现象。
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

    // 当播放页面重新打开时，重置滑动偏移量及手势状态，确保页面能正常显示且手势状态正确
    LaunchedEffect(isPlayerOpen) {
        if (isPlayerOpen) {
            offsetY = 0f
            isScrollGestureActive = false
            isGestureStartedAtTop = true
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
            // 如果手势是在顶部封面区或顶栏开始的，允许使用滑动速度与偏移共同判断是否关闭
            // 如果是从下方列表滑回顶部并拉起页面，则忽略惯性滑动速度以防误触，仅允许刻意下拉超过阈值时关闭
            val shouldClose = if (isGestureStartedAtTop) {
                offsetY > screenHeightPx * 0.20f || velocity > 1000f
            } else {
                offsetY > screenHeightPx * 0.20f
            }

            if (offsetY > 0f && shouldClose) {
                animate(
                    initialValue = offsetY,
                    targetValue = screenHeightPx,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ ->
                    offsetY = value.coerceAtLeast(0f)
                }
                onClose()
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
                    offsetY = value.coerceAtLeast(0f) // 限制非负，防止弹簧动画回弹时产生负偏移导致页面上移
                }
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 如果是用户手势输入且手势状态尚未激活，则在此处捕获手势起点是否在最顶部（主页）
                if (source == NestedScrollSource.UserInput) {
                    if (!isScrollGestureActive) {
                        isScrollGestureActive = true
                        // 放宽判定条件：只要列表首个可见项为封面项（Index == 0），即认为手势始于顶部主页区，消除微小位移导致阻尼失效的异常感
                        isGestureStartedAtTop = listState.firstVisibleItemIndex == 0
                    }
                }

                return if (offsetY > 0f && available.y < 0f) {
                    // 向上滑动回弹时：若从最顶部（主页）开始滑动则不加阻尼（1.0f），若从下方滑动则应用 0.3f 阻尼
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
                // 如果是用户手势输入且手势状态尚未激活，则在此处捕获手势起点是否在最顶部（主页）
                if (source == NestedScrollSource.UserInput) {
                    if (!isScrollGestureActive) {
                        isScrollGestureActive = true
                        isGestureStartedAtTop = listState.firstVisibleItemIndex == 0
                    }
                }

                // 只有当列表滑动到最顶部且为用户直接拖动时，下滑操作才增加 offsetY 展开/关闭页面，防止在列表底部越界回弹或其他位置触发下滑关闭
                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                return if (available.y > 0f && isAtTop && source == NestedScrollSource.UserInput) {
                    // 下滑操作：若从最顶部（主页）开始滑动则不加阻尼（1.0f），若从下方滑动到最顶部继续下滑则应用 0.3f 阻尼，防止快速滑动误触退出
                    val damping = if (isGestureStartedAtTop) 1.0f else 0.3f
                    offsetY += available.y * damping
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // 手势结束，重置手势状态
                isScrollGestureActive = false
                // 只有当页面已被向下拉开时才拦截并处理释放逻辑，避免列表正常滑动时被误判
                return if (offsetY > 0f) {
                    handleDragRelease(velocity = available.y)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // 手势结束，重置手势状态
                isScrollGestureActive = false
                // 只有当页面已被向下拉开时才拦截并处理释放逻辑，避免列表正常滑动时被误判
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
            .background(BackgroundDark)
    ) {
        // 专辑背景的动态光效与封面绑定，使用渐变色向下方大幅延伸并随封面滚动平移，最终自然淡出至透明以消除边界硬切线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1200.dp) // 增加光效容器高度，确保向下方大幅延伸并避免截断
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
            // 封面图
            item(key = "cover") {
                CoverArt(
                    coverUrl = coverUrl,
                    title = title,
                    playContext = playContext,
                    onClose = onClose,
                    onPaletteExtracted = { colorPalette = it },
                    modifier = Modifier.graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                    }
                )
            }

            // 歌曲信息
            item(key = "song_info") {
                SongInfo(
                    title = title,
                    artist = artist,
                    isLiked = isLiked,
                    onToggleLike = viewModel::toggleLike
                )
            }

            // 单行歌词预览
            item(key = "mini_lyric") {
                MiniLyricLine(
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    lyricsHighlight = lyricsHighlight
                )
            }

            // 进度条
            item(key = "progress") {
                ProgressSection(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek
                )
            }

            // 播放控制
            item(key = "controls") {
                PlaybackControls(
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlay,
                    onPlayNext = viewModel.playerManager::playNext,
                    onPlayPrevious = viewModel.playerManager::playPrevious,
                    onToggleShuffle = viewModel.playerManager::toggleShuffle,
                    onToggleRepeat = viewModel.playerManager::toggleRepeat,
                    playMode = playMode
                )
            }

            // 功能按钮行
            item(key = "actions") {
                ActionButtons(onQueueClick = { showQueueSheet = true })
            }

            // 歌词卡片（纯音乐不显示）
            val isPureMusic = lyrics.size == 1 && lyrics[0].text == "纯音乐"
            if (isLyricsLoading || (lyrics.isNotEmpty() && !isPureMusic)) {
                item(key = "lyrics") {
                    LyricsCard(
                        lyrics = lyrics,
                        currentIndex = currentLyricIndex,
                        isLoading = isLyricsLoading,
                        cardBrush = lyricsCardBrush,
                        highlightColor = lyricsHighlight
                    )
                }
            }

            // 评论预览卡片
            item(key = "comments_preview") {
                CommentsPreviewCard(
                    commentsState = commentsState,
                    cardColor = SurfaceDark,
                    onRetry = viewModel::retryComments
                )
            }

            // 歌曲详情卡片
            item(key = "song_detail") {
                SongDetailCard(songWiki = songWiki, songDetail = songDetail, cardColor = SurfaceDark)
            }

            // 关于艺人卡片
            item(key = "about_artist") {
                AboutArtistCard(
                    artistDetail = artistDetail,
                    fansCount = artistFansCount,
                    cardColor = SurfaceDark
                )
            }

            // 相似艺人卡片
            item(key = "similar_artists") {
                SimilarArtistsCard(
                    artists = similarArtists,
                    isLoading = isSimilarArtistsLoading,
                    cardColor = SurfaceDark
                )
            }

            // 艺人专辑卡片
            item(key = "artist_albums") {
                ArtistAlbumsCard(
                    albums = artistAlbums,
                    artistName = artistDetail?.name,
                    cardColor = SurfaceDark
                )
            }
        }

        // 固定顶栏覆盖层
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
            modifier = Modifier.draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // 顶栏拖拽为直接关闭操作，不加阻尼以保持手感顺畅
                    offsetY = (offsetY + delta).coerceAtLeast(0f)
                },
                onDragStarted = {
                    // 直接下拉顶栏时，强制认定为从顶部开始的关闭动作，允许快速惯性滑动关闭
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
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}

@Composable
private fun TopBar(
    onClose: () -> Unit,
    title: String = "",
    artist: String = "",
    showTitle: Boolean = false,
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {},
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {},
    backgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    if (showTitle) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleLike) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) NeteaseRed else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverArt(
    coverUrl: String,
    title: String,
    playContext: String?,
    onClose: () -> Unit,
    onPaletteExtracted: (ColorPalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = if (playContext != null) "正在播放：$playContext" else "NOW PLAYING",
                color = if (playContext != null) TextGray else Color.White,
                fontSize = if (playContext != null) 13.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (playContext != null) 0.sp else 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(coverUrl.ifEmpty { null })
                .allowHardware(false)
                .crossfade(true)
                .build(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                onPaletteExtracted(extractColorPalette(state.result.drawable))
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun SongInfo(title: String, artist: String, isLiked: Boolean, onToggleLike: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                color = TextGray.copy(alpha = 0.7f),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggleLike) {
            Icon(
                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isLiked) NeteaseRed else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun MiniLyricLine(
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    lyricsHighlight: Color
) {
    val isPureMusic = lyrics.size == 1 && lyrics[0].text == "纯音乐"
    val currentText = when {
        isPureMusic -> "纯音乐"
        lyrics.isNotEmpty() && currentLyricIndex in lyrics.indices -> lyrics[currentLyricIndex].text
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = currentText,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "mini_lyric"
        ) { text ->
            Text(
                text = text.ifEmpty { "· · ·" },
                color = if (text.isEmpty()) TextGray.copy(alpha = 0.4f) else lyricsHighlight.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSection(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) {
        if (isSeeking) seekPosition else currentPosition.toFloat() / duration
    } else 0f

    val thumbSize by animateDpAsState(
        targetValue = if (isSeeking) 14.dp else 6.dp,
        animationSpec = tween(150),
        label = "thumb_size"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = {
                isSeeking = true
                seekPosition = it
            },
            onValueChangeFinished = {
                isSeeking = false
                onSeek((seekPosition * duration).toLong())
            },
            thumb = {
                // 使用默认的 20.dp 大小作为容器并进行内容居中，避免自定义较小的 thumbSize 导致在 M3 Slider 中对齐偏上
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .background(Color.White, CircleShape)
                    )
                }
            },
            track = { sliderState ->
                val fraction = sliderState.value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(SurfaceLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPosition = if (isSeeking) (seekPosition * duration).toLong() else currentPosition
            Text(formatTime(displayPosition), color = TextGray, fontSize = 12.sp)
            Text(formatTime(duration), color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    playMode: PlayMode
) {
    val bounceScale = remember { Animatable(1f) }
    LaunchedEffect(isPlaying) {
        bounceScale.snapTo(0.85f)
        bounceScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = null,
                tint = if (playMode == PlayMode.SHUFFLE) Color.White else TextGray,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onPlayPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        FloatingActionButton(
            onClick = onTogglePlay,
            containerColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = bounceScale.value
                    scaleY = bounceScale.value
                }
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = onPlayNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = onToggleRepeat) {
            Icon(
                if (playMode == PlayMode.SINGLE_LOOP) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = null,
                tint = if (playMode == PlayMode.SINGLE_LOOP) Color.White else TextGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ActionButtons(onQueueClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(onClick = { }) {
                Icon(Icons.Rounded.Devices, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
            }
        }
        Row {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Share, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onQueueClick) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LyricsCard(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    isLoading: Boolean,
    cardBrush: Brush,
    highlightColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // 1:1 square card
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "歌词",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Rounded.OpenInFull,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                LyricsPreview(
                    lyrics = lyrics,
                    currentIndex = currentIndex,
                    highlightColor = highlightColor
                )
            }
        }
    }
}

@Composable
private fun LyricsPreview(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    highlightColor: Color
) {
    // ── Layout constants ────────────────────────────────────────────────────
    val itemSpacingDp = 14.dp

    // Every lyric row that has a translation always reserves that space so item
    // heights never change at runtime.  This is the key invariant that allows
    // animateScrollToItem to use a precise, stable offset.
    //
    // A row with no translation:          mainLine (≈28.dp at 20.sp × scale)
    // A row WITH translation:             mainLine + 4.dp spacer + transLine (≈15.sp)
    //
    // We use the same pair of estimates as before for the scroll math, but because
    // heights are now stable the numbers are always accurate.
    val itemHeightNoDpEst    = 36.dp
    val itemHeightWithTransDpEst = 56.dp

    // ── Centre-scroll threshold ─────────────────────────────────────────────
    // Once currentIndex reaches this value the list starts scrolling to keep the
    // active row centred.  Before that, lyrics fill from the top.
    val itemStrideDp      = itemHeightNoDpEst + itemSpacingDp   // ≈ 50.dp

    val density       = LocalDensity.current
    val lazyListState = rememberLazyListState()

    // Derive card height from the LazyColumn's measured size rather than a
    // hard-coded constant, so it tracks the parent 1:1 square correctly.
    var cardHeightPx by remember { mutableFloatStateOf(0f) }

    // ── Scroll control ──────────────────────────────────────────────────────
    // Phase 1 (currentIndex < threshold): don't scroll; highlight in place.
    // Phase 2 (currentIndex >= threshold): smoothly keep active row centred.
    LaunchedEffect(currentIndex) {
        if (currentIndex < 0 || currentIndex >= lyrics.size) return@LaunchedEffect

        val cardHeightDp = with(density) { cardHeightPx.toDp() }
        val linesAboveCentre = (cardHeightDp / 2 / itemStrideDp).toInt()

        if (currentIndex < linesAboveCentre) {
            // Phase 1 – reset to top when song changes.
            lazyListState.animateScrollToItem(index = 0, scrollOffset = 0)
            return@LaunchedEffect
        }
        // Phase 2 – centre the active row.
        // Height is stable (translation always reserved), so offset is always exact.
        val hasTranslation   = lyrics[currentIndex].translation != null
        val itemHeightPx     = with(density) {
            if (hasTranslation) itemHeightWithTransDpEst.toPx() else itemHeightNoDpEst.toPx()
        }
        // Negative offset pulls the item up so its midpoint aligns with the viewport centre.
        val centreOffsetPx = -(((cardHeightPx - itemHeightPx) / 2f).toInt())
        lazyListState.animateScrollToItem(
            index       = currentIndex,
            scrollOffset = centreOffsetPx
        )
    }

    // ── Render ──────────────────────────────────────────────────────────────
    LazyColumn(
        state   = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()  // fills the remaining height inside the 1:1 Card Column
            .onSizeChanged { cardHeightPx = it.height.toFloat() },
        verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
        userScrollEnabled   = false,
        // No top padding → lyrics start at the top.
        // Bottom half-card padding → last item can scroll to the centre.
        contentPadding = PaddingValues(top = 0.dp, bottom = with(density) { (cardHeightPx / 2).toDp() })
    ) {
        itemsIndexed(items = lyrics, key = { i, _ -> i }) { index, line ->
            val isCurrent = index == currentIndex
            val distance  = kotlin.math.abs(index - currentIndex).coerceAtMost(4)

            // ── graphicsLayer-only transitions (Draw phase, GPU, no layout cost) ──

            // Scale: active 1.15 → falls to 0.85 for distant lines.
            val targetScale = if (isCurrent) 1.15f
                              else (1f - distance * 0.07f).coerceAtLeast(0.85f)
            val animatedScale by animateFloatAsState(
                targetValue   = targetScale,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label         = "lyric_scale_$index"
            )

            // Alpha for the main lyric text.
            val targetAlpha = if (isCurrent) 1f
                              else (0.55f - distance * 0.1f).coerceAtLeast(0.2f)
            val animatedAlpha by animateFloatAsState(
                targetValue   = targetAlpha,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label         = "lyric_alpha_$index"
            )

            // Alpha for the translation row (always rendered to keep layout stable).
            val targetTransAlpha = if (isCurrent) 0.65f else 0f
            val animatedTransAlpha by animateFloatAsState(
                targetValue   = targetTransAlpha,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label         = "lyric_trans_alpha_$index"
            )

            // NO animateContentSize – item heights are now invariant.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha  = animatedAlpha
                        // Scale from the left edge to match TextAlign.Start.
                        transformOrigin =
                            androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
            ) {
                Text(
                    text       = line.text,
                    fontSize   = 20.sp,   // 常量 - 视觉大小调整通过 scaleX/Y 实现
                    color      = if (isCurrent) Color.White else highlightColor, // 激活歌词使用白色，未激活使用高亮主题色
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                    textAlign  = TextAlign.Start,
                    modifier   = Modifier.fillMaxWidth()
                )
                // 翻译在有歌词时始终存在于布局中 - 仅通过 graphicsLayer 更改其 alpha。这保持了行高的稳定性。
                if (line.translation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text      = line.translation,
                        fontSize  = 15.sp,
                        // 通过 graphicsLayer 驱动可见性，而不是切换组合项，因此布局高度永远不会改变。
                        color     = Color.White.copy(alpha = animatedTransAlpha), // 激活的翻译使用带透明度的白色
                        textAlign = TextAlign.Start,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SongDetailCard(
    songWiki: SongWikiData?,
    songDetail: Track?,
    cardColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "歌曲详情",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (songWiki == null) {
                // 如果还在加载中，展示加载指示器
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                // 属性列布局
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 曲风
                    if (songWiki.style.isNotEmpty()) {
                        SongDetailRow(label = "曲风", value = songWiki.style)
                    }

                    // 专辑
                    val albumName = songWiki.album.ifEmpty { songDetail?.al?.name.orEmpty() }
                    if (albumName.isNotEmpty()) {
                        SongDetailRow(label = "专辑", value = albumName)
                    }

                    // 语种
                    if (songWiki.language.isNotEmpty()) {
                        SongDetailRow(label = "语种", value = songWiki.language)
                    }

                    // 发行时间
                    val publishDate = songWiki.publishTime.ifEmpty {
                        val time = songDetail?.publishTime ?: 0L
                        if (time > 0L) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(time))
                        } else ""
                    }
                    if (publishDate.isNotEmpty()) {
                        SongDetailRow(label = "发行时间", value = publishDate)
                    }

                    // BPM
                    if (songWiki.bpm.isNotEmpty()) {
                        SongDetailRow(label = "BPM", value = songWiki.bpm)
                    }

                    // 影综
                    if (songWiki.entertainment.isNotEmpty()) {
                        SongDetailRow(label = "影综", value = songWiki.entertainment)
                    }

                    // 制作
                    if (songWiki.creators.isNotEmpty()) {
                        SongDetailRow(
                            label = "制作",
                            value = songWiki.creators,
                            showChevron = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongDetailRow(
    label: String,
    value: String,
    showChevron: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左边固定宽度的标签名，实现完美的整齐对齐
        Text(
            text = label,
            color = TextGray.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(70.dp)
        )

        // 右侧内容
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SimilarArtistsCard(
    artists: List<ArtistInfo>,
    isLoading: Boolean,
    cardColor: Color
) {
    if (artists.isEmpty() && !isLoading) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(
                "探索类似艺人",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(72.dp)
                        ) {
                            AsyncImage(
                                model = "${artist.avatarUrl}?param=150y150",
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artist.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutArtistCard(
    artistDetail: ArtistDetailInfo?,
    fansCount: Long?,
    cardColor: Color
) {
    if (artistDetail == null) return

    val coverUrl = artistDetail.cover.ifEmpty { artistDetail.avatar }
    if (coverUrl.isBlank()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column {
            // 顶部艺人图片容器，高度约220dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = artistDetail.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 顶部半透明渐变罩，确保“关于艺人”文本可读
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
                // “关于艺人”标题，位于图片左上角
                Text(
                    text = "关于艺人",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }

            // 底部内容区域，包含名字、关注按钮和简介
            Column(modifier = Modifier.padding(20.dp)) {
                // 艺人名字和关注按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = artistDetail.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // 显示歌手粉丝数，若粉丝数为空则显示占位符
                        Text(
                            text = if (fansCount != null) {
                                "${formatFansCount(fansCount)}粉丝"
                            } else {
                                "--粉丝"
                            },
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                    // 关注按钮，带白边半透明样式
                    var isFollowed by remember { mutableStateOf(false) } // 模拟关注状态
                    OutlinedButton(
                        onClick = { isFollowed = !isFollowed },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Transparent
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            width = 1.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (isFollowed) "已关注" else "关注",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 艺人简介，末尾带“查看更多”或“收起”字样，支持展开与收回
                if (artistDetail.briefDesc.isNotBlank()) {
                    var isExpanded by remember { mutableStateOf(false) }
                    val cleanDesc = artistDetail.briefDesc.trim()
                    val annotatedText = buildAnnotatedString {
                        val maxLen = 95
                        if (cleanDesc.length > maxLen && !isExpanded) {
                            val truncated = cleanDesc.take(maxLen)
                            append(truncated)
                            append("... ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("查看更多")
                            }
                        } else if (isExpanded) {
                            append(cleanDesc)
                            append(" ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("收起")
                            }
                        } else {
                            append(cleanDesc)
                        }
                    }

                    Text(
                        text = annotatedText,
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (cleanDesc.length > 95) {
                                    isExpanded = !isExpanded
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbumsCard(
    albums: List<ArtistAlbum>,
    artistName: String?,
    cardColor: Color
) {
    if (albums.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(
                text = if (artistName != null) "${artistName}的更多专辑" else "更多专辑",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    Column(
                        modifier = Modifier.width(120.dp)
                    ) {
                        AsyncImage(
                            model = "${album.picUrl}?param=250y250",
                            contentDescription = album.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = album.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun CommentsPreviewCard(
    commentsState: CommentsState,
    cardColor: Color,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Comment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "评论",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (commentsState is CommentsState.Success) {
                        Text(
                            text = "(${commentsState.total})",
                            color = TextGray.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (commentsState) {
                is CommentsState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeteaseRed,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                is CommentsState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "加载评论失败: ${commentsState.message}",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        TextButton(
                            onClick = onRetry,
                            colors = ButtonDefaults.textButtonColors(contentColor = NeteaseRed)
                        ) {
                            Text("重试", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is CommentsState.Success -> {
                    val allComments = (commentsState.hotComments + commentsState.comments)
                        .distinctBy { it.commentId }
                        .take(3)

                    if (allComments.isEmpty()) {
                        Text(
                            text = "暂无评论，分享你的第一条感受吧~",
                            color = TextGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            allComments.forEachIndexed { index, comment ->
                                CommentRowItem(comment = comment)
                                if (index < allComments.size - 1) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.08f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Footer
                        Text(
                            text = "查看全部 ${commentsState.total} 条评论",
                            color = NeteaseRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRowItem(comment: CommentItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = "${comment.user.avatarUrl}?param=80y80",
            contentDescription = comment.user.nickname,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = comment.user.nickname,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = comment.timeStr ?: "",
                        color = TextGray.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatLikedCount(comment.likedCount),
                        color = TextGray.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = comment.content,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatLikedCount(count: Int): String {
    return when {
        count >= 100_000 -> "${count / 10_000}w+"
        count >= 10_000 -> String.format(java.util.Locale.getDefault(), "%.1fw", count / 10000f)
        count >= 1000 -> "${count / 1000}k+"
        else -> count.toString()
    }
}

// 格式化粉丝数量，如果超过10000则显示为“X万”，保留一位小数并去掉末尾的“.0”
private fun formatFansCount(count: Long): String {
    return if (count >= 10000) {
        val countDouble = count / 10000.0
        val formatted = String.format(java.util.Locale.US, "%.1f", countDouble)
        if (formatted.endsWith(".0")) {
            formatted.substring(0, formatted.length - 2) + "万"
        } else {
            formatted + "万"
        }
    } else {
        count.toString()
    }
}



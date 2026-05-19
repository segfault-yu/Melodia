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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.ArtistInfo
import com.lin0721.linmusic.data.repository.LyricLine
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

@Composable
fun FullPlayerScreen(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit
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
    val artistAlbums by viewModel.artistAlbums.collectAsStateWithLifecycle()
    val playContext by viewModel.playerManager.playContext.collectAsStateWithLifecycle()

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

    val gradientStart = animatedDominant.toOpaqueHsv(minSat = 0.75f, valRange = 0.6f..0.85f)
    val gradientEnd = animatedSecondary.toOpaqueHsv(minSat = 1.0f, valRange = 0.3f..0.3f)
    val lyricsCardBrush = Brush.linearGradient(colors = listOf(gradientEnd, gradientStart))

    val lyricsHighlight = lerp(start = animatedDominant, stop = Color.White, fraction = 0.85f)

    val infiniteTransition = rememberInfiniteTransition(label = "bg_breathe")
    val gradientEndY by infiniteTransition.animateFloat(
        initialValue = 1600f,
        targetValue = 2000f,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedDominant, animatedSecondary, BackgroundDark),
                    startY = 0f,
                    endY = gradientEndY
                )
            )
            .statusBarsPadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp)
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
                SongInfo(title = title, artist = artist)
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
                    onTogglePlay = onTogglePlay
                )
            }

            // 功能按钮行
            item(key = "actions") {
                ActionButtons()
            }

            // 歌词卡片（纯音乐不显示）
            if (isLyricsLoading || lyrics.isNotEmpty()) {
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

            // 关于艺人卡片
            item(key = "about_artist") {
                AboutArtistCard(artistDetail = artistDetail, cardColor = SurfaceDark)
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

            // 制作人卡片
            item(key = "credits") {
                CreditsCard(songDetail = songDetail, cardColor = SurfaceDark)
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
            backgroundColor = animatedDominant
        )
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
    backgroundColor: Color = Color.Transparent
) {
    if (showTitle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
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
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
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
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
private fun SongInfo(title: String, artist: String) {
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
        IconButton(onClick = { }) {
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
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
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .background(Color.White, CircleShape)
                )
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
    onTogglePlay: () -> Unit
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
        IconButton(onClick = { }) {
            Icon(Icons.Default.Shuffle, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = { }) {
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
        IconButton(onClick = { }) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.Repeat, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ActionButtons() {
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
            IconButton(onClick = { }) {
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
            .clip(RoundedCornerShape(16.dp))
            .background(cardBrush)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .height(300.dp)
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
    val visibleRange = 5
    val startIndex = (currentIndex - visibleRange).coerceAtLeast(0)
    val endIndex = (currentIndex + visibleRange).coerceAtMost(lyrics.size - 1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (i in startIndex..endIndex) {
            val line = lyrics[i]
            val isCurrent = i == currentIndex
            val distance = kotlin.math.abs(i - currentIndex)

            val targetAlpha = if (isCurrent) 1f else (0.6f - distance * 0.1f).coerceAtLeast(0.25f)
            val animatedAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(400),
                label = "lyric_alpha_$i"
            )
            val targetSize = if (isCurrent) 22f else 16f
            val animatedSize by animateFloatAsState(
                targetValue = targetSize,
                animationSpec = tween(350),
                label = "lyric_size_$i"
            )

            Column {
                Text(
                    text = line.text,
                    color = if (isCurrent) highlightColor else Color.White.copy(alpha = animatedAlpha),
                    fontSize = animatedSize.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isCurrent && line.translation != null) {
                    Text(
                        text = line.translation,
                        color = highlightColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditsCard(songDetail: Track?, cardColor: Color) {
    if (songDetail == null) return

    val artists = songDetail.ar
    if (artists.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "制作人",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "显示全部",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            artists.forEachIndexed { index, artist ->
                val avatarUrl = "${artist.img1v1Url.ifEmpty { artist.picUrl }}?param=100y100"
                val role = if (index == 0) "主要艺人" else "艺人"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = artist.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = role,
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
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
private fun AboutArtistCard(artistDetail: ArtistDetailInfo?, cardColor: Color) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = artistDetail.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        "关于艺人",
                        color = TextGray,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        artistDetail.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                if (artistDetail.briefDesc.isNotBlank()) {
                    Text(
                        text = artistDetail.briefDesc.take(100) + if (artistDetail.briefDesc.length > 100) "..." else "",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Text("关注", fontSize = 13.sp)
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

package com.lin0721.linmusic.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import com.lin0721.linmusic.player.PlayMode
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.data.repository.LyricLine
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.TextGray
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsCard(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    isLoading: Boolean,
    gradientStart: Color,
    gradientEnd: Color,
    accentColor: Color,
    highlightColor: Color,
    onOpenFullScreen: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_mesh")

    // 左上角光源动画
    val accentCenterX by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_x"
    )
    val accentCenterY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_y"
    )
    val accentRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_radius"
    )

    // 右下角光源动画
    val whiteCenterX by infiniteTransition.animateFloat(
        initialValue = 1.20f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_x"
    )
    val whiteCenterY by infiniteTransition.animateFloat(
        initialValue = 1.20f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_y"
    )
    val whiteRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_radius"
    )

    val cardWidth = (LocalConfiguration.current.screenWidthDp - 32).dp
    val cardHeight = cardWidth * 0.88f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(cardHeight)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val baseSize = size.minDimension
                // 1. 填充基底
                drawRect(color = gradientEnd)
                
                // 2. 左上角光雾
                val accentRadius = baseSize * accentRadiusScale
                val accentCenter = Offset(size.width * accentCenterX, size.height * accentCenterY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.8f), Color.Transparent),
                        center = accentCenter,
                        radius = accentRadius
                    ),
                    center = accentCenter,
                    radius = accentRadius
                )
                
                // 3. 右下角光雾
                val whiteRadius = baseSize * whiteRadiusScale
                val whiteCenter = Offset(size.width * whiteCenterX, size.height * whiteCenterY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(gradientStart.copy(alpha = 0.7f), Color.Transparent),
                        center = whiteCenter,
                        radius = whiteRadius
                    ),
                    center = whiteCenter,
                    radius = whiteRadius
                )
            }
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
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onOpenFullScreen() }
                        .padding(4.dp)
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
fun LyricsPreview(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    highlightColor: Color
) {
    val itemSpacingDp = 14.dp
    val itemHeightNoDpEst    = 36.dp
    val itemHeightWithTransDpEst = 56.dp
    val itemStrideDp      = itemHeightNoDpEst + itemSpacingDp

    val density       = LocalDensity.current
    val lazyListState = rememberLazyListState()
    var cardHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentIndex) {
        if (currentIndex < 0 || currentIndex >= lyrics.size) return@LaunchedEffect

        val cardHeightDp = with(density) { cardHeightPx.toDp() }
        val linesAboveCentre = (cardHeightDp / 2 / itemStrideDp).toInt()

        if (currentIndex < linesAboveCentre) {
            lazyListState.animateScrollToItem(index = 0, scrollOffset = 0)
            return@LaunchedEffect
        }
        val hasTranslation   = lyrics[currentIndex].translation != null
        val itemHeightPx     = with(density) {
            if (hasTranslation) itemHeightWithTransDpEst.toPx() else itemHeightNoDpEst.toPx()
        }
        val centreOffsetPx = -(((cardHeightPx - itemHeightPx) / 2f).toInt())
        lazyListState.animateScrollToItem(
            index       = currentIndex,
            scrollOffset = centreOffsetPx
        )
    }

    LazyColumn(
        state   = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .onSizeChanged { cardHeightPx = it.height.toFloat() },
        verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
        userScrollEnabled   = false,
        contentPadding = PaddingValues(top = 0.dp, bottom = with(density) { (cardHeightPx / 2).toDp() })
    ) {
        itemsIndexed(items = lyrics, key = { i, _ -> i }) { index, line ->
            val isCurrent = index == currentIndex
            val distance  = kotlin.math.abs(index - currentIndex).coerceAtMost(4)

            val targetScale = if (isCurrent) 1.15f
                              else (1f - distance * 0.07f).coerceAtLeast(0.85f)
            val animatedScale by animateFloatAsState(
                targetValue   = targetScale,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label         = "lyric_scale_$index"
            )

            val targetAlpha = if (isCurrent) 1f
                              else (0.55f - distance * 0.1f).coerceAtLeast(0.2f)
            val animatedAlpha by animateFloatAsState(
                targetValue   = targetAlpha,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label         = "lyric_alpha_$index"
            )

            val targetTransAlpha = 0.65f
            val animatedTransAlpha by animateFloatAsState(
                targetValue   = targetTransAlpha,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label         = "lyric_trans_alpha_$index"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha  = animatedAlpha
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) {
                Text(
                    text       = line.text,
                    fontSize   = 20.sp,
                    color      = if (isCurrent) Color.White else highlightColor,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Start,
                    modifier   = Modifier.fillMaxWidth()
                )
                if (line.translation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text      = line.translation,
                        fontSize  = 15.sp,
                        color     = Color.White.copy(alpha = animatedTransAlpha),
                        textAlign = TextAlign.Start,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenLyricsView(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    isLoading: Boolean,
    title: String,
    artist: String,
    gradientStart: Color,
    gradientEnd: Color,
    accentColor: Color,
    highlightColor: Color,
    isUserScrolling: Boolean,
    onUserScrollingChanged: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    hazeState: HazeState,
    onClose: () -> Unit,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    playMode: PlayMode,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onMoreClick: () -> Unit
) {
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    var offsetY by remember { mutableStateOf(0f) }
    var isScrollGestureActive by remember { mutableStateOf(false) }
    var isGestureStartedAtTop by remember { mutableStateOf(true) }
    var dragReleaseJob by remember { mutableStateOf<Job?>(null) }

    fun handleDragRelease(velocity: Float = 0f) {
        dragReleaseJob?.cancel()
        dragReleaseJob = scope.launch {
            val shouldClose = if (isGestureStartedAtTop) {
                offsetY > viewportHeightPx * 0.20f || velocity > 1000f
            } else {
                offsetY > viewportHeightPx * 0.20f
            }

            if (offsetY > 0f && shouldClose) {
                animate(
                    initialValue = offsetY,
                    targetValue = viewportHeightPx,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ ->
                    offsetY = value
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
                        isGestureStartedAtTop = lazyListState.firstVisibleItemIndex == 0
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
                        isGestureStartedAtTop = lazyListState.firstVisibleItemIndex == 0
                    }
                }

                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
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

    val gestureModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    timerJob?.cancel()
                    onUserScrollingChanged(true)
                } else if (event.type == PointerEventType.Release) {
                    timerJob?.cancel()
                    timerJob = scope.launch {
                        delay(5000)
                        onUserScrollingChanged(false)
                    }
                }
            }
        }
    }

    LaunchedEffect(currentIndex, isUserScrolling, viewportHeightPx) {
        if (!isUserScrolling && currentIndex in lyrics.indices && viewportHeightPx > 0f) {
            val itemStridePx = with(density) { 66.dp.toPx() }
            val linesAboveCentre = (viewportHeightPx / 2 / itemStridePx).toInt()

            if (currentIndex < linesAboveCentre) {
                lazyListState.animateScrollToItem(index = 0, scrollOffset = 0)
                return@LaunchedEffect
            }

            val hasTranslation = lyrics[currentIndex].translation != null
            val itemHeightPx = with(density) {
                if (hasTranslation) 96.dp.toPx() else 54.dp.toPx()
            }
            val centreOffsetPx = -((viewportHeightPx - itemHeightPx) / 2f).toInt()
            lazyListState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = centreOffsetPx
            )
        }
    }

    val centerLineIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf -1
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            var minDistance = Float.MAX_VALUE
            var closestIndex = -1
            for (item in visibleItems) {
                val itemCenter = item.offset + item.size / 2f
                val distance = kotlin.math.abs(itemCenter - viewportCenter)
                if (distance < minDistance) {
                    minDistance = distance
                    closestIndex = item.index
                }
            }
            closestIndex
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fluid_mesh_fullscreen")

    val accentCenterX by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_x"
    )
    val accentCenterY by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_y"
    )
    val accentRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_radius"
    )

    val whiteCenterX by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_x"
    )
    val whiteCenterY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_y"
    )
    val whiteRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "white_radius"
    )

    val topCornerRadius by remember {
        derivedStateOf {
            if (offsetY > 0f) 24.dp else 0.dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .graphicsLayer {
                translationY = offsetY
            }
            .clip(RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius))
            .drawBehind {
                val baseSize = size.minDimension
                drawRect(color = gradientEnd)
                
                val accentRadius = baseSize * accentRadiusScale
                val accentCenter = Offset(size.width * accentCenterX, size.height * accentCenterY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.6f), Color.Transparent),
                        center = accentCenter,
                        radius = accentRadius
                    ),
                    center = accentCenter,
                    radius = accentRadius
                )
                
                val whiteRadius = baseSize * whiteRadiusScale
                val whiteCenter = Offset(size.width * whiteCenterX, size.height * whiteCenterY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(gradientStart.copy(alpha = 0.5f), Color.Transparent),
                        center = whiteCenter,
                        radius = whiteRadius
                    ),
                    center = whiteCenter,
                    radius = whiteRadius
                )
            }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeChild(state = hazeState, style = HazeStyle(blurRadius = 40.dp, noiseFactor = 0.02f))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .draggable(
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
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "折叠歌词",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        modifier = Modifier.size(32.dp).align(Alignment.Center)
                    )
                } else if (lyrics.isEmpty()) {
                    Text(
                        text = "暂无歌词",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    CenterTargetLine(
                        visible = isUserScrolling,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.Center)
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(gestureModifier)
                            .onSizeChanged { viewportHeightPx = it.height.toFloat() },
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = with(density) { (viewportHeightPx / 2f).toDp() }
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(items = lyrics, key = { i, _ -> i }) { index, line ->
                            val isCurrent = index == currentIndex
                            val isCenterTarget = index == centerLineIndex && isUserScrolling
                            val distance = kotlin.math.abs(index - currentIndex).coerceAtMost(5)

                            val targetScale = if (isCurrent) 1.15f
                                              else if (isCenterTarget) 1.05f
                                              else (1f - distance * 0.05f).coerceAtLeast(0.82f)
                            val animatedScale by animateFloatAsState(
                                targetValue = targetScale,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                label = "fs_lyric_scale_$index"
                            )

                            val targetAlpha = if (isCurrent) 1f
                                              else if (isCenterTarget) 0.85f
                                              else (0.65f - distance * 0.08f).coerceAtLeast(0.2f)
                            val animatedAlpha by animateFloatAsState(
                                targetValue = targetAlpha,
                                animationSpec = tween(250),
                                label = "fs_lyric_alpha_$index"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp)
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        alpha = animatedAlpha
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                                    .clickable {
                                        timerJob?.cancel()
                                        onUserScrollingChanged(false)
                                        onSeek(line.timeMs)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isCurrent && line.words.isNotEmpty()) {
                                    KaraokeLyricRow(
                                        line = line,
                                        currentPositionMs = currentPosition,
                                        inactiveColor = Color.White.copy(alpha = 0.35f),
                                        activeColor = Color.White,
                                        fontSize = 22.sp
                                    )
                                } else {
                                    Text(
                                        text = line.text,
                                        fontSize = 22.sp,
                                        color = if (isCurrent) Color.White else highlightColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (line.translation != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = line.translation,
                                        fontSize = 17.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    PlayCapsule(
                        visible = isUserScrolling && centerLineIndex in lyrics.indices,
                        targetLine = lyrics.getOrNull(centerLineIndex),
                        onSeek = onSeek,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp)
                    )

                    BackToPlayingButton(
                        visible = isUserScrolling,
                        onClick = { onUserScrollingChanged(false) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp)
                    )
                }
            }

            FullScreenControls(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = onSeek,
                onTogglePlay = onTogglePlay,
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                playMode = playMode,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat
            )
        }
    }
}

@Composable
private fun CenterTargetLine(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun PlayCapsule(
    visible: Boolean,
    targetLine: LyricLine?,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        if (targetLine != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable {
                        onSeek(targetLine.timeMs)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "跳转到此处播放",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime(targetLine.timeMs),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BackToPlayingButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    onClick()
                }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CenterFocusWeak,
                contentDescription = "回到正在播放",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "返回正在播放",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    playMode: PlayMode,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) {
        if (isSeeking) seekPosition else currentPosition.toFloat() / duration
    } else 0f

    val bounceScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isPlaying) {
        bounceScale.snapTo(0.85f)
        bounceScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
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
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSeeking) 14.dp else 8.dp)
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
                        .background(Color.White.copy(alpha = 0.2f))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPosition = if (isSeeking) (seekPosition * duration).toLong() else currentPosition
            Text(formatTime(displayPosition), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text(formatTime(duration), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用 Box 替代 IconButton，消除 48dp 隐式内边距，让图标贴近进度条左边缘
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onToggleShuffle() }
                    .padding(start = 0.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = if (playMode == PlayMode.SHUFFLE) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPrevious) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = bounceScale.value
                            scaleY = bounceScale.value
                        }
                        .background(Color.White, CircleShape)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        tint = Color.Black,
                        modifier = Modifier.size(42.dp),
                        contentDescription = null
                    )
                }

                IconButton(onClick = onPlayNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // 同理，让 Repeat 图标贴近进度条右边缘
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onToggleRepeat() },
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (playMode == PlayMode.SINGLE_LOOP) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = null,
                    tint = if (playMode == PlayMode.SINGLE_LOOP) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun KaraokeLyricRow(
    line: LyricLine,
    currentPositionMs: Long,
    inactiveColor: Color,
    activeColor: Color,
    fontSize: TextUnit = 22.sp
) {
    var textLayoutResult by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val currentPositionState = rememberUpdatedState(currentPositionMs)
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 底层灰色（未激活）歌词
        Text(
            text = line.text,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = inactiveColor,
            textAlign = TextAlign.Center,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 顶层高亮（已激活）歌词，通过裁剪只显示已唱完的物理区域
        Text(
            text = line.text,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = activeColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val layout = textLayoutResult
                    if (layout == null) {
                        alpha = 0f
                    } else {
                        alpha = 1f
                        val progressPx = calculatePhysicalProgressPx(
                            line = line,
                            currentPositionMs = currentPositionState.value,
                            textLayoutResult = layout
                        )
                        clip = true
                        shape = object : Shape {
                            override fun createOutline(
                                size: Size,
                                layoutDirection: LayoutDirection,
                                density: Density
                            ): Outline {
                                return Outline.Rectangle(
                                    Rect(0f, 0f, progressPx, size.height)
                                )
                            }
                        }
                    }
                }
        )
    }
}

private fun calculatePhysicalProgressPx(
    line: LyricLine,
    currentPositionMs: Long,
    textLayoutResult: TextLayoutResult
): Float {
    val totalWidth = textLayoutResult.size.width.toFloat()
    
    // 若没有字词时间或时间总长为 0，则根据时间戳退化为整行切换
    if (line.words.isEmpty() || line.durationMs <= 0L) {
        return when {
            currentPositionMs < line.timeMs -> 0f
            currentPositionMs >= line.timeMs + line.durationMs -> totalWidth
            else -> 0f
        }
    }

    val relativeProgress = currentPositionMs - line.timeMs
    if (relativeProgress <= 0) return 0f
    
    // 获取歌词文本的实际渲染几何边界
    val lineLeft = textLayoutResult.getLineLeft(0)
    val lineRight = textLayoutResult.getLineRight(0)
    val actualTextWidth = lineRight - lineLeft
    
    if (relativeProgress >= line.durationMs) return totalWidth

    // 预先映射每个 word 在整行文本中的索引区间
    val textLength = line.text.length
    var currentSearchIndex = 0
    val wordRanges = line.words.map { word ->
        val startIndex = line.text.indexOf(word.text, currentSearchIndex)
        val range = if (startIndex != -1) {
            currentSearchIndex = startIndex + word.text.length
            startIndex until currentSearchIndex
        } else {
            val start = currentSearchIndex
            currentSearchIndex = (currentSearchIndex + word.text.length).coerceAtMost(textLength)
            start until currentSearchIndex
        }
        range
    }

    var playedWidth = 0f

    for (i in line.words.indices) {
        val word = line.words[i]
        val range = wordRanges[i]
        val wordStart = word.startOffsetMs
        val wordEnd = wordStart + word.durationMs
        
        // 尝试获取该词首尾字符的实际物理坐标
        val wordLeft = try {
            textLayoutResult.getBoundingBox(range.first).left
        } catch (e: Exception) {
            textLayoutResult.getHorizontalPosition(range.first, true)
        }
        val wordRight = try {
            textLayoutResult.getBoundingBox(range.last).right
        } catch (e: Exception) {
            textLayoutResult.getHorizontalPosition(range.last + 1, true)
        }
        
        if (relativeProgress >= wordEnd) {
            playedWidth = wordRight - lineLeft
        } else if (relativeProgress in wordStart..wordEnd) {
            // 在词内进行时间物理宽度插值
            val wordProgress = (relativeProgress - wordStart).toFloat() / word.durationMs
            val currentWordWidth = wordRight - wordLeft
            val wordPlayedWidth = currentWordWidth * wordProgress.coerceIn(0f, 1f)
            playedWidth = (wordLeft - lineLeft) + wordPlayedWidth
            break
        } else {
            break
        }
    }
    
    return (lineLeft + playedWidth).coerceIn(0f, totalWidth)
}

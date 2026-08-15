package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.player.PlayMode
import com.lin0721.linmusic.core.player.domain.LyricLine
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ────────────────────────────────────────────────────────────────────────────
// 全屏歌词页：承载下拉关闭手势与滚动跟随状态，装配顶栏、歌词列表与播放控制
// ────────────────────────────────────────────────────────────────────────────
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
    onSeek: (Long) -> Unit,
    hazeState: HazeState,
    onClose: () -> Unit,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    playMode: PlayMode,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onMoreClick: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    var offsetY by remember { mutableStateOf(0f) }
    var isScrollGestureActive by remember { mutableStateOf(false) }
    var isGestureStartedAtTop by remember { mutableStateOf(true) }
    var dragReleaseJob by remember { mutableStateOf<Job?>(null) }
    // 歌词页手势拖动的纯 UI 交互态，不涉及业务数据，只在本组件内部使用
    var isUserScrolling by remember { mutableStateOf(false) }

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

    val isPlayingState = rememberUpdatedState(isPlaying)

    // 拖动/点击跳转播放进度后，同时结束用户滚动态，恢复自动跟随当前歌词行
    val handleSeek: (Long) -> Unit = { timeMs ->
        isUserScrolling = false
        onSeek(timeMs)
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (isUserScrolling) {
                isUserScrolling = false
            }
        } else {
            timerJob?.cancel()
        }
    }

    val gestureModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    timerJob?.cancel()
                    isUserScrolling = true
                } else if (event.type == PointerEventType.Release) {
                    timerJob?.cancel()
                    if (isPlayingState.value) {
                        timerJob = scope.launch {
                            delay(5000)
                            isUserScrolling = false
                        }
                    }
                }
            }
        }
    }

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
            .fullScreenLyricsBackground(
                gradientStart = gradientStart,
                gradientEnd = gradientEnd,
                accentColor = accentColor
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeChild(state = hazeState, style = HazeStyle(blurRadius = 40.dp, noiseFactor = 0.02f))
                .statusBarsPadding()
        ) {
            FullScreenLyricsHeader(
                title = title,
                artist = artist,
                onClose = onClose,
                onMoreClick = onMoreClick,
                onDragDelta = { delta ->
                    offsetY = (offsetY + delta).coerceAtLeast(0f)
                },
                onDragStart = {
                    isGestureStartedAtTop = true
                },
                onDragRelease = { velocity ->
                    handleDragRelease(velocity = velocity)
                }
            )

            FullScreenLyricsList(
                lyrics = lyrics,
                currentIndex = currentIndex,
                isLoading = isLoading,
                isUserScrolling = isUserScrolling,
                highlightColor = highlightColor,
                currentPositionProvider = currentPositionProvider,
                lazyListState = lazyListState,
                viewportHeightPx = viewportHeightPx,
                onViewportHeightChange = { height ->
                    viewportHeightPx = height
                },
                gestureModifier = gestureModifier,
                onSeek = handleSeek,
                onLyricClick = { line ->
                    timerJob?.cancel()
                    handleSeek(line.timeMs)
                }
            )

            FullScreenControls(
                isPlaying = isPlaying,
                currentPositionProvider = currentPositionProvider,
                duration = duration,
                onSeek = handleSeek,
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

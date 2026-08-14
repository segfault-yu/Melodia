package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

// ────────────────────────────────────────────────────────────────────────────
// 搜索栏二态吸附：只有主动下拉超过阈值才放行到 item 0，fling 一律弹回 item 1
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun rememberSearchBarSnapConnection(
    listState: LazyListState,
    isDailyRecommend: Boolean
): NestedScrollConnection {
    val density = LocalDensity.current

    // 下拉阈值超过 40dp 触发弹出
    val snapThresholdPx = with(density) { 40.dp.toPx() }
    var pullAccumulator by remember { mutableFloatStateOf(0f) }
    // 标记是否通过主动拖拽打开，防止 fling 误入 item 0 后被误判为已打开
    var intentionallyOpened by remember { mutableStateOf(false) }

    val searchBarSnapConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isDailyRecommend) return Offset.Zero

                // 在 item 1 顶部边界
                if (available.y > 0 && listState.firstVisibleItemIndex == 1
                    && listState.firstVisibleItemScrollOffset == 0
                ) {
                    if (source == NestedScrollSource.UserInput) {
                        pullAccumulator += available.y
                        if (pullAccumulator >= snapThresholdPx) {
                            pullAccumulator = 0f
                            intentionallyOpened = true
                            // 阈值达到，放行让后续拖拽自然滚到 item 0
                            // gesture 持有 scroll mutex，此时不能调用 animateScrollToItem
                            return Offset.Zero
                        }
                    }
                    // 未达阈值的拖拽 或 fling：消耗全部，停在 item 1
                    return available
                }

                if (available.y < 0) {
                    pullAccumulator = 0f
                    // 向上滑动收起搜索栏时重置标记
                    if (listState.firstVisibleItemIndex >= 1) intentionallyOpened = false
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullAccumulator = 0f
                // 手指已抬起，gesture 结束，mutex 已释放，此处调用动画安全
                // 若已达阈值但 list 还没滚到 item 0，补充动画
                if (intentionallyOpened && listState.firstVisibleItemIndex != 0) {
                    listState.animateScrollToItem(0)
                    return available // 消耗 fling 速度，由动画接管
                }
                return Velocity.Zero
            }

            // 如果 fling 仍然越过了边界进入 item 0，强制 snap 回 item 1
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!isDailyRecommend && listState.firstVisibleItemIndex == 0 && !intentionallyOpened) {
                    listState.animateScrollToItem(1)
                }
                return Velocity.Zero
            }
        }
    }

    // 慢速拖拽后的吸附：手势结束时若搜索栏处于中间态，自动就近吸附
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && !isDailyRecommend
            && listState.firstVisibleItemIndex == 0
        ) {
            val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            val itemHeight = itemInfo?.size ?: return@LaunchedEffect
            if (listState.firstVisibleItemScrollOffset > itemHeight / 2) {
                intentionallyOpened = false
                listState.animateScrollToItem(1)
            } else {
                listState.animateScrollToItem(0)
            }
        }
    }

    return searchBarSnapConnection
}

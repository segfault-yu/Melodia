package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// 由列表滚动位置推导的视觉指标：顶栏标题显隐、封面缩放、背景层位移
@Stable
class FullPlayerScrollMetrics(private val listState: LazyListState) {

    // 封面滚出视野后没有实测尺寸，沿用最后一次记录的高度做估算
    private var coverHeight by mutableStateOf(1000f)

    val showTitleInBar by derivedStateOf { listState.firstVisibleItemIndex > 0 }

    val backgroundTranslationY by derivedStateOf {
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

@Composable
fun rememberFullPlayerScrollMetrics(listState: LazyListState): FullPlayerScrollMetrics =
    remember(listState) { FullPlayerScrollMetrics(listState) }

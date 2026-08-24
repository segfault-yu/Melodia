package com.lin0721.linmusic.feature.music.ui

import com.lin0721.linmusic.feature.music.domain.MusicStyle
import com.lin0721.linmusic.feature.music.domain.StyleContent
import com.lin0721.linmusic.feature.music.domain.StylePreference

// 顶部曲风胶囊选中项。「我的偏好」是一个虚拟项，不对应任何 tagId
sealed interface StyleSelection {
    data object Preference : StyleSelection
    data class Style(val id: Long) : StyleSelection
}

// 「音乐」tab UI 状态
sealed interface MusicUiState {
    data object Loading : MusicUiState

    data class Success(val data: MusicFeedData) : MusicUiState

    data class Error(val message: String) : MusicUiState
}

data class MusicFeedData(
    val styles: List<MusicStyle> = emptyList(),
    // 未登录或无数据时为空，此时不展示「我的偏好」胶囊
    val preferences: List<StylePreference> = emptyList(),
    val selection: StyleSelection = StyleSelection.Preference,
    // 当前一级曲风下选中的二级标签，null 表示「全部」
    val selectedChildId: Long? = null,
    val content: StyleContent? = null,
    // 切换曲风时只刷新内容区，顶部胶囊保持可点
    val isContentLoading: Boolean = false
) {
    val hasPreference: Boolean get() = preferences.isNotEmpty()

    // 当前实际请求内容用的 tagId：优先二级标签，其次一级曲风，偏好页取占比最高的曲风
    val activeTagId: Long?
        get() = selectedChildId ?: when (val current = selection) {
            is StyleSelection.Style -> current.id
            StyleSelection.Preference -> preferences.maxByOrNull { it.ratio }?.id
        }

    val selectedStyle: MusicStyle?
        get() = (selection as? StyleSelection.Style)?.let { sel -> styles.firstOrNull { it.id == sel.id } }
}

package com.lin0721.linmusic.feature.cloud.ui

import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.cloud.domain.CloudQuota
import com.lin0721.linmusic.feature.cloud.domain.CloudSong

// 我的云盘主列表 UI 状态
sealed interface CloudUiState {
    data object Loading : CloudUiState

    data class Success(
        val songs: List<CloudSong>,
        val quota: CloudQuota,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : CloudUiState

    data class Error(val message: String) : CloudUiState
}

// 三点菜单/删除确认/重新匹配面板，同一时间只会有一个浮层展示，用一个状态承载
sealed interface CloudOverlay {
    data object Hidden : CloudOverlay
    data class Options(val song: CloudSong) : CloudOverlay
    data class DeleteConfirm(val song: CloudSong, val isDeleting: Boolean = false) : CloudOverlay
    data class Match(
        val song: CloudSong,
        val query: String = "",
        val results: List<Track> = emptyList(),
        val isSearching: Boolean = false,
        // 选中候选曲目后先展示二次确认，不直接调用匹配接口
        val confirmTarget: Track? = null,
        val isMatching: Boolean = false
    ) : CloudOverlay
}

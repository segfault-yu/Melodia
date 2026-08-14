package com.lin0721.linmusic.feature.playlist.ui

import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.Track

// 歌单/专辑详情页 UI 状态
sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data class Success(
        val playlist: PlaylistDetail,
        val recommendedSongs: List<Track> = emptyList(),
        val isSubscribed: Boolean = false
    ) : PlaylistUiState
    data class Error(val message: String) : PlaylistUiState
}

// 历史日推（每日推荐/听歌排行）的浏览状态，独立于页面主加载态
data class HistoryRecommendState(
    val dates: List<String> = emptyList(),
    val datesLoading: Boolean = false,
    val songs: List<DailySong> = emptyList(),
    val selectedDate: String? = null,
    val songsLoading: Boolean = false
)

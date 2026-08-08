package com.lin0721.linmusic.ui.playlist

import com.lin0721.linmusic.data.remote.api.DailySong
import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.Track

sealed interface PlaylistUiState {
    object Loading : PlaylistUiState
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

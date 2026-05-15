package com.lin0721.linmusic.ui.home

import com.lin0721.linmusic.data.remote.api.PersonalizedPlaylist
import com.lin0721.linmusic.data.remote.api.Artist
import com.lin0721.linmusic.data.remote.api.RecentPlayItem
import com.lin0721.linmusic.data.remote.api.DailySong

// 首页 UI 状态
sealed interface HomeUiState {
    data object Loading : HomeUiState

    // 加载成功，携带聚合后的首页数据
    data class Success(val data: HomeFeedData) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

// 首页聚合数据
data class HomeFeedData(
    val recommendPlaylists: List<PersonalizedPlaylist>,
    val topArtists: List<Artist>,
    val recentPlaylists: List<RecentPlayItem> = emptyList(),
    val dailySongs: List<DailySong> = emptyList()
)

package com.lin0721.linmusic.feature.home.ui

import com.lin0721.linmusic.feature.home.data.PersonalizedPlaylist
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.feature.home.data.RecentPlayItem
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.feature.home.domain.ToplistInfo

import com.lin0721.linmusic.core.model.ArtistInfo

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
    val favoriteArtists: List<ArtistInfo> = emptyList(),
    val recentPlaylists: List<RecentPlayItem> = emptyList(),
    val dailySongs: List<DailySong> = emptyList(),
    val toplistItems: List<ToplistInfo> = emptyList()
)

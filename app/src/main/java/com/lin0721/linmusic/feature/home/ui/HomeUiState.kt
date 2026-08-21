package com.lin0721.linmusic.feature.home.ui

import com.lin0721.linmusic.feature.home.data.PersonalizedPlaylist
import com.lin0721.linmusic.feature.home.data.RecentPlayItem
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.feature.home.domain.HomeShelf
import com.lin0721.linmusic.feature.home.domain.ToplistInfo

// 首页 UI 状态
sealed interface HomeUiState {
    data object Loading : HomeUiState

    // 加载成功，携带聚合后的首页数据
    data class Success(val data: HomeFeedData) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

// 首页聚合数据。货架序列由服务端编排下发，其余几项供顶部固定区块使用
data class HomeFeedData(
    val shelves: List<HomeShelf> = emptyList(),
    val recommendPlaylists: List<PersonalizedPlaylist> = emptyList(),
    val recentPlaylists: List<RecentPlayItem> = emptyList(),
    val dailySongs: List<DailySong> = emptyList(),
    val toplistItems: List<ToplistInfo> = emptyList(),
    // 翻页游标，null 表示服务端已无更多货架
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false
)

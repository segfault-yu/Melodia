package com.lin0721.linmusic.feature.podcast.ui

import com.lin0721.linmusic.feature.podcast.domain.PodcastCategory
import com.lin0721.linmusic.feature.podcast.domain.PodcastProgram
import com.lin0721.linmusic.feature.podcast.domain.PodcastRadio
import com.lin0721.linmusic.feature.podcast.domain.PodcastRadioDetail

// 「播客」tab UI 状态
sealed interface PodcastUiState {
    data object Loading : PodcastUiState

    data class Success(val data: PodcastFeedData) : PodcastUiState

    data class Error(val message: String) : PodcastUiState
}

data class PodcastFeedData(
    val categories: List<PodcastCategory> = emptyList(),
    // null 表示「推荐」，不限分类
    val selectedCategoryId: Long? = null,
    val programs: List<PodcastProgram> = emptyList(),
    // 未登录时为空，整段不展示
    val personalizedRadios: List<PodcastRadio> = emptyList(),
    val recommendRadios: List<PodcastRadio> = emptyList(),
    val toplistRadios: List<PodcastRadio> = emptyList(),
    // 切分类时只刷新节目列表，货架保持不动
    val isProgramLoading: Boolean = false
)

// 电台详情页 UI 状态
sealed interface RadioDetailUiState {
    data object Loading : RadioDetailUiState

    data class Success(
        val detail: PodcastRadioDetail,
        val programs: List<PodcastProgram>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        // 订阅请求进行中，按钮置为等待态防止连点
        val isSubscribing: Boolean = false
    ) : RadioDetailUiState

    data class Error(val message: String) : RadioDetailUiState
}

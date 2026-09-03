package com.lin0721.linmusic.feature.newworks.ui

import com.lin0721.linmusic.feature.newworks.domain.NewWorksMv
import com.lin0721.linmusic.feature.newworks.domain.NewWorksRelease

// 首页音乐 tab「最新」二级药丸的内容区状态
sealed interface NewWorksUiState {
    data object Loading : NewWorksUiState

    data class Success(
        val mvs: List<NewWorksMv>,
        val releases: List<NewWorksRelease>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean
    ) : NewWorksUiState

    data class Error(val message: String) : NewWorksUiState
}

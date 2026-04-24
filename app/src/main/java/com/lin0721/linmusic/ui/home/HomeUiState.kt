package com.lin0721.linmusic.ui.home

import com.lin0721.linmusic.data.remote.api.RecommendPlaylistData

/**
 * 首页 UI 状态密封接口
 *
 * 用于描述首页数据加载的三种状态，供 Compose UI 层进行分支渲染。
 */
sealed interface HomeUiState {

    /** 加载中 */
    data object Loading : HomeUiState

    /** 加载成功，携带推荐歌单数据 */
    data class Success(val data: RecommendPlaylistData) : HomeUiState

    /** 加载失败，携带错误消息 */
    data class Error(val message: String) : HomeUiState
}

package com.lin0721.linmusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 *
 * 通过构造函数注入 [MusicRepository]，在初始化时自动发起个性化推荐歌单请求，
 * 并将结果映射为 [HomeUiState] 暴露给 Compose UI 层。
 */
class HomeViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

    /** UI 层可观察的状态流（只读） */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPersonalizedPlaylists()
    }

    /**
     * 加载个性化推荐歌单
     *
     * 收集 Repository 返回的 Flow<Result>，将其映射为对应 UiState。
     */
    fun loadPersonalizedPlaylists() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            musicRepository.getPersonalizedPlaylists().collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { data -> HomeUiState.Success(data) },
                    onFailure = { error ->
                        HomeUiState.Error(
                            error.localizedMessage ?: "未知错误"
                        )
                    }
                )
            }
        }
    }
}

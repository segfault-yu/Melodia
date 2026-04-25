package com.lin0721.linmusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lin0721.linmusic.player.PlayerManager

/**
 * 首页 ViewModel
 *
 * 通过构造函数注入 [MusicRepository]，在初始化时自动发起个性化推荐歌单请求，
 * 并将结果映射为 [HomeUiState] 暴露给 Compose UI 层。
 */
class HomeViewModel(
    private val musicRepository: MusicRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadPersonalizedPlaylists()
        viewModelScope.launch {
            playerManager.initController()
        }
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

    fun playSong(songId: Long, title: String, artist: String, coverUrl: String) {
        viewModelScope.launch {
            musicRepository.getSongUrl(songId).collect { result ->
                result.onSuccess { url ->
                    playerManager.playAudio(url, title, artist, coverUrl)
                }.onFailure { error ->
                    _toastEvent.emit(error.message ?: "无法获取播放链接")
                }
            }
        }
    }
    
    fun togglePlayPause() {
        if (playerManager.isPlaying.value) {
            playerManager.pause()
        } else {
            playerManager.resume()
        }
    }
}

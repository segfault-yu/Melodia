package com.lin0721.linmusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.data.remote.api.AccountInfoResponse
import com.lin0721.linmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 用户信息状态流，从 DataStore 实时读取
    val userProfile: StateFlow<UserProfile?> = userPreferences.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadHomeData()
        viewModelScope.launch {
            playerManager.initController()
        }
    }

    /**
     * 加载首页聚合数据（并发获取歌单和歌手）
     */
    fun loadHomeData() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            try {
                // 1. 获取推荐歌单 (核心数据)
                val playlistsDeferred = async { 
                    musicRepository.getPersonalizedPlaylists().first()
                }
                
                // 2. 获取热门歌手 (非核心数据，独立容错)
                val artistsDeferred = async { 
                    runCatching { musicRepository.getTopArtists().first() }
                        .getOrElse { Result.success(emptyList()) }
                }

                // 3. 获取最近播放 (非核心数据，独立容错)
                val recentDeferred = async {
                    runCatching { musicRepository.getRecentPlaylists().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val playlistsResult = playlistsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val recentResult = recentDeferred.await()

                if (playlistsResult.isSuccess) {
                    _uiState.value = HomeUiState.Success(
                        HomeFeedData(
                            recommendPlaylists = playlistsResult.getOrThrow().playlists,
                            topArtists = artistsResult.getOrDefault(emptyList()),
                            recentPlaylists = recentResult.getOrDefault(emptyList())
                        )
                    )
                } else {
                    _uiState.value = HomeUiState.Error(
                        playlistsResult.exceptionOrNull()?.message ?: "加载核心数据失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "未知错误")
            }
        }
    }

    fun playSong(songId: Long, title: String, artist: String, coverUrl: String, startPosition: Long = 0) {
        viewModelScope.launch {
            musicRepository.getSongUrl(songId).collect { result ->
                result.onSuccess { url ->
                    playerManager.playAudio(songId, url, title, artist, coverUrl, startPosition)
                }.onFailure { error ->
                    _toastEvent.emit(error.message ?: "无法获取播放链接")
                }
            }
        }
    }
    
    fun togglePlayPause() {
        val currentTrack = playerManager.currentTrack.value
        if (!playerManager.isPlaying.value && currentTrack != null) {
            val songId = currentTrack.mediaMetadata.extras?.getLong("songId") ?: -1L
            // 如果 MediaItem 只有元数据而没有实际的 URI (说明是持久化恢复的占位符)
            if (songId != -1L && currentTrack.localConfiguration == null) {
                playSong(
                    songId = songId,
                    title = currentTrack.mediaMetadata.title?.toString() ?: "",
                    artist = currentTrack.mediaMetadata.artist?.toString() ?: "",
                    coverUrl = currentTrack.mediaMetadata.artworkUri?.toString() ?: "",
                    startPosition = playerManager.currentPosition.value
                )
                return
            }
        }
        playerManager.togglePlayPause()
    }

    /**
     * 处理登录成功
     * 1. 保存 Cookie
     * 2. 拉取真实的账号与用户信息
     * 3. 持久化用户信息并刷新首页
     */
    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            // 1. 保存 Cookie
            userPreferences.saveCookies(cookies)
            
            // 2. 拉取真实账号信息
            musicRepository.getAccountInfo().collect { result ->
                val response = result.getOrNull()
                if (response != null) {
                    val remoteProfile = response.profile
                    if (remoteProfile != null) {
                        // 3. 保存用户信息 (此处创建的是本地 UserProfile 模型)
                        userPreferences.saveUserProfile(
                            UserProfile(
                                uid = remoteProfile.userId,
                                nickname = remoteProfile.nickname,
                                avatarUrl = remoteProfile.avatarUrl
                            )
                        )
                        _toastEvent.emit("登录成功，欢迎回来，${remoteProfile.nickname}！")
                        // 4. 刷新数据
                        loadHomeData()
                    } else {
                        _toastEvent.emit("登录成功，但未能获取用户信息")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _toastEvent.emit("同步用户信息失败: ${error?.message}")
                }
            }
        }
    }

    /**
     * 退出登录
     * 清除 DataStore 中的用户信息
     */
    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserProfile()
            _toastEvent.emit("已退出登录")
            // 退出登录后也刷新下首页（可能需要切换回免登录接口）
            loadHomeData()
        }
    }
}

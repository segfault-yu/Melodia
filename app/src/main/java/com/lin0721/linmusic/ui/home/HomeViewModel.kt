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
 */
class HomeViewModel(
    private val musicRepository: MusicRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    fun loadHomeData() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            try {
                val playlistsDeferred = async { 
                    musicRepository.getPersonalizedPlaylists().first()
                }
                
                val artistsDeferred = async { 
                    runCatching { musicRepository.getTopArtists().first() }
                        .getOrElse { Result.success(emptyList()) }
                }

                val recentDeferred = async {
                    runCatching { musicRepository.getRecentPlaylists().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val fmDeferred = async {
                    runCatching { musicRepository.getPersonalFm().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val playlistsResult = playlistsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val recentResult = recentDeferred.await()
                val fmResult = fmDeferred.await()

                if (playlistsResult.isSuccess) {
                    _uiState.value = HomeUiState.Success(
                        HomeFeedData(
                            recommendPlaylists = playlistsResult.getOrThrow().playlists,
                            topArtists = artistsResult.getOrDefault(emptyList()),
                            recentPlaylists = recentResult.getOrDefault(emptyList()),
                            personalFm = fmResult.getOrDefault(emptyList())
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

    fun playPersonalFm() {
        val state = uiState.value
        if (state is HomeUiState.Success && state.data.personalFm.isNotEmpty()) {
            val track = state.data.personalFm[0]
            playSong(
                songId = track.id,
                title = track.name,
                artist = track.ar.joinToString { it.name },
                coverUrl = track.al.picUrl
            )
        } else {
            viewModelScope.launch { _toastEvent.emit("私人 FM 暂无歌曲") }
        }
    }

    fun likeSong(trackId: Long, like: Boolean) {
        viewModelScope.launch {
            musicRepository.likeSong(trackId, like).collect { result ->
                result.onSuccess {
                    _toastEvent.emit(if (like) "已收藏" else "已取消收藏")
                }.onFailure { e ->
                    _toastEvent.emit("操作失败: ${e.message}")
                }
            }
        }
    }

    fun trashFmSong(songId: Long) {
        viewModelScope.launch {
            musicRepository.trashFmSong(songId).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("已不再播放此歌曲")
                    loadHomeData() // 刷新 FM 列表
                }.onFailure { e ->
                    _toastEvent.emit("操作失败: ${e.message}")
                }
            }
        }
    }
    
    fun togglePlayPause() {
        val currentTrack = playerManager.currentTrack.value
        if (!playerManager.isPlaying.value && currentTrack != null) {
            val songId = currentTrack.mediaMetadata.extras?.getLong("songId") ?: -1L
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

    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            userPreferences.saveCookies(cookies)
            musicRepository.getAccountInfo().collect { result ->
                val response = result.getOrNull()
                if (response != null) {
                    val remoteProfile = response.profile
                    if (remoteProfile != null) {
                        userPreferences.saveUserProfile(
                            UserProfile(
                                uid = remoteProfile.userId,
                                nickname = remoteProfile.nickname,
                                avatarUrl = remoteProfile.avatarUrl
                            )
                        )
                        _toastEvent.emit("登录成功，欢迎回来，${remoteProfile.nickname}！")
                        loadHomeData()
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserProfile()
            _toastEvent.emit("已退出登录")
            loadHomeData()
        }
    }
}

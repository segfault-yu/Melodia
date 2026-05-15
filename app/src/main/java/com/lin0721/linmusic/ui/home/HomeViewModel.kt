package com.lin0721.linmusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.data.remote.api.AccountInfoResponse
import com.lin0721.linmusic.data.remote.api.DailySong
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

// 首页 ViewModel
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

    // 历史日推状态
    private val _historyDates = MutableStateFlow<List<String>>(emptyList())
    val historyDates: StateFlow<List<String>> = _historyDates.asStateFlow()

    private val _historyDatesLoading = MutableStateFlow(false)
    val historyDatesLoading: StateFlow<Boolean> = _historyDatesLoading.asStateFlow()

    private val _historySongs = MutableStateFlow<List<DailySong>>(emptyList())
    val historySongs: StateFlow<List<DailySong>> = _historySongs.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _historySongsLoading = MutableStateFlow(false)
    val historySongsLoading: StateFlow<Boolean> = _historySongsLoading.asStateFlow()

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

                val dailySongsDeferred = async {
                    runCatching { musicRepository.getDailyRecommendSongs().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val playlistsResult = playlistsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val recentResult = recentDeferred.await()
                val dailySongsResult = dailySongsDeferred.await()

                if (playlistsResult.isSuccess) {
                    _uiState.value = HomeUiState.Success(
                        HomeFeedData(
                            recommendPlaylists = playlistsResult.getOrThrow().playlists,
                            topArtists = artistsResult.getOrDefault(emptyList()),
                            recentPlaylists = recentResult.getOrDefault(emptyList()),
                            dailySongs = dailySongsResult.getOrDefault(emptyList())
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

    fun playDailySong(index: Int = 0) {
        val state = uiState.value
        if (state is HomeUiState.Success && state.data.dailySongs.isNotEmpty()) {
            val song = state.data.dailySongs.getOrElse(index) { state.data.dailySongs[0] }
            playSong(
                songId = song.id,
                title = song.name,
                artist = song.ar.joinToString { it.name },
                coverUrl = song.al.picUrl
            )
        } else {
            viewModelScope.launch { _toastEvent.emit("每日推荐暂无歌曲") }
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

    // 加载历史日推可用日期
    fun loadHistoryDates() {
        viewModelScope.launch {
            _historyDatesLoading.value = true
            musicRepository.getHistoryRecommendDates().collect { result ->
                result.onSuccess { dates ->
                    _historyDates.value = dates
                    // 自动加载第一个日期的详情
                    if (dates.isNotEmpty() && _selectedDate.value == null) {
                        loadHistoryDetail(dates.first())
                    }
                }.onFailure {
                    _toastEvent.emit("历史日推需要黑胶会员")
                }
                _historyDatesLoading.value = false
            }
        }
    }

    // 加载指定日期的历史日推歌曲
    fun loadHistoryDetail(date: String) {
        viewModelScope.launch {
            _selectedDate.value = date
            _historySongsLoading.value = true
            musicRepository.getHistoryRecommendDetail(date).collect { result ->
                result.onSuccess { songs ->
                    _historySongs.value = songs
                }.onFailure {
                    _toastEvent.emit("加载失败：${it.message}")
                }
                _historySongsLoading.value = false
            }
        }
    }

    // 播放历史推荐中的歌曲
    fun playHistorySong(index: Int) {
        val songs = _historySongs.value
        if (songs.isEmpty()) return
        val song = songs.getOrElse(index) { songs[0] }
        playSong(
            songId = song.id,
            title = song.name,
            artist = song.ar.joinToString { it.name },
            coverUrl = song.al.picUrl
        )
    }
}

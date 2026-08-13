package com.lin0721.linmusic.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.api.AccountInfoResponse
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.core.auth.AuthRepository
import com.lin0721.linmusic.feature.artist.data.ArtistRepository
import com.lin0721.linmusic.feature.home.data.HomeRepository
import com.lin0721.linmusic.feature.player.data.PlayerRepository
import com.lin0721.linmusic.feature.home.domain.ToplistInfo
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
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
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem

// 首页 ViewModel
class HomeViewModel(
    private val playerRepository: PlayerRepository,
    private val homeRepository: HomeRepository,
    private val artistRepository: ArtistRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider
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
                    homeRepository.getPersonalizedPlaylists().first()
                }

                val artistsDeferred = async {
                    runCatching { artistRepository.getFavoriteArtists().first() }
                        .getOrElse { Result.success(emptyList()) }
                }

                val recentDeferred = async {
                    runCatching { homeRepository.getRecentPlaylists().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val dailySongsDeferred = async {
                    runCatching { homeRepository.getDailyRecommendSongs().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val toplistDeferred = async {
                    runCatching { homeRepository.getToplistDetail().first() }
                        .getOrDefault(Result.success(emptyList<ToplistInfo>()))
                }

                val playlistsResult = playlistsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val recentResult = recentDeferred.await()
                val dailySongsResult = dailySongsDeferred.await()
                val toplistResult = toplistDeferred.await()

                if (playlistsResult.isSuccess) {
                    _uiState.value = HomeUiState.Success(
                        HomeFeedData(
                            recommendPlaylists = playlistsResult.getOrThrow().playlists,
                            favoriteArtists = artistsResult.getOrDefault(emptyList()),
                            recentPlaylists = recentResult.getOrDefault(emptyList()),
                            dailySongs = dailySongsResult.getOrDefault(emptyList()),
                            toplistItems = toplistResult.getOrDefault(emptyList())
                        )
                    )
                } else {
                    _uiState.value = HomeUiState.Error(
                        playlistsResult.exceptionOrNull()?.toUserMessage(resourceProvider)
                            ?: resourceProvider.getString(R.string.app_error_biz_default)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.toUserMessage(resourceProvider))
            }
        }
    }

    fun playSong(songId: Long, title: String, artist: String, coverUrl: String, startPosition: Long = 0, playContext: String? = null) {
        viewModelScope.launch {
            playerRepository.getSongUrl(songId).collect { result ->
                result.onSuccess { url ->
                    playerManager.playAudio(songId, url, title, artist, coverUrl, startPosition, playContext)
                }.onFailure { error ->
                    _toastEvent.emit(error.toUserMessage(resourceProvider))
                }
            }
        }
    }

    fun playDailySong(index: Int = 0) {
        val state = uiState.value
        if (state is HomeUiState.Success && state.data.dailySongs.isNotEmpty()) {
            val queueItems = state.data.dailySongs.map { song ->
                QueueItem(song.id, song.name, song.ar.joinToString { it.name }, song.al.picUrl)
            }
            playerManager.playQueue(queueItems, index.coerceIn(0, queueItems.size - 1), "每日推荐")
        } else {
            viewModelScope.launch { _toastEvent.emit("每日推荐暂无歌曲") }
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            userPreferences.saveCookies(cookies)
            authRepository.getAccountInfo().collect { result ->
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

    // 开启相似歌曲漫游
    fun startRoaming() {
        val current = playerManager.currentTrack.value
        if (current != null) {
            val songId = current.mediaId?.toLongOrNull() ?: return
            val title = current.mediaMetadata.title?.toString() ?: ""
            val artist = current.mediaMetadata.artist?.toString() ?: ""
            val coverUrl = current.mediaMetadata.artworkUri?.toString() ?: ""
            viewModelScope.launch {
                playerRepository.getSimilarSongs(songId).collect { result ->
                    result.onSuccess { simiSongs ->
                        if (simiSongs.isNotEmpty()) {
                            val currentItem = QueueItem(songId, title, artist, coverUrl)
                            val simiItems = simiSongs.map { track ->
                                QueueItem(
                                    songId = track.id,
                                    title = track.name,
                                    artist = track.ar.joinToString("/") { it.name },
                                    coverUrl = track.al.picUrl
                                )
                            }
                            val roamingQueue = listOf(currentItem) + simiItems
                            playerManager.playQueue(roamingQueue, 0, playContext = "similar_roaming")
                            _toastEvent.emit("已开启相似歌曲漫游")
                        } else {
                            _toastEvent.emit("未找到相关相似歌曲")
                        }
                    }.onFailure {
                        _toastEvent.emit(it.toUserMessage(resourceProvider))
                    }
                }
            }
        } else {
            val state = uiState.value
            if (state is HomeUiState.Success && state.data.dailySongs.isNotEmpty()) {
                val firstSong = state.data.dailySongs.first()
                viewModelScope.launch {
                    playerRepository.getSimilarSongs(firstSong.id).collect { result ->
                        result.onSuccess { simiSongs ->
                            val currentItem = QueueItem(firstSong.id, firstSong.name, firstSong.ar.joinToString("/") { it.name }, firstSong.al.picUrl)
                            val simiItems = simiSongs.map { track ->
                                QueueItem(track.id, track.name, track.ar.joinToString("/") { it.name }, track.al.picUrl)
                            }
                            val roamingQueue = listOf(currentItem) + simiItems
                            playerManager.playQueue(roamingQueue, 0, playContext = "similar_roaming")
                            _toastEvent.emit("已为您开启《${firstSong.name}》的漫游")
                        }.onFailure {
                            _toastEvent.emit(it.toUserMessage(resourceProvider))
                        }
                    }
                }
            } else {
                viewModelScope.launch { _toastEvent.emit("播放列表中没有歌曲且无法获取今日推荐") }
            }
        }
    }

    // 开启心动模式
    fun startIntelligenceMode() {
        val current = playerManager.currentTrack.value
        if (current != null) {
            val songId = current.mediaId?.toLongOrNull() ?: return
            viewModelScope.launch {
                playerRepository.getIntelligenceSongs(songId, 0).collect { result ->
                    result.onSuccess { tracks ->
                        if (tracks.isNotEmpty()) {
                            val currentItem = QueueItem(
                                songId,
                                current.mediaMetadata.title?.toString() ?: "",
                                current.mediaMetadata.artist?.toString() ?: "",
                                current.mediaMetadata.artworkUri?.toString() ?: ""
                            )
                            val items = listOf(currentItem) + tracks.map { track ->
                                QueueItem(track.id, track.name, track.ar.joinToString("/") { it.name }, track.al.picUrl)
                            }
                            playerManager.playQueue(items, 0, playContext = "intelligence")
                            _toastEvent.emit("已开启心动模式")
                        } else {
                            _toastEvent.emit("获取心动推荐失败")
                        }
                    }.onFailure {
                        _toastEvent.emit(it.toUserMessage(resourceProvider))
                    }
                }
            }
        } else {
                                    val state = uiState.value
                                        if (state is HomeUiState.Success && state.data.dailySongs.isNotEmpty()) {
                                            val firstSong = state.data.dailySongs.first()
                                            viewModelScope.launch {
                                                playerRepository.getIntelligenceSongs(firstSong.id, 0).collect { result ->
                                                    result.onSuccess { tracks ->
                                                        val currentItem = QueueItem(firstSong.id, firstSong.name, firstSong.ar.joinToString("/") { it.name }, firstSong.al.picUrl)
                                                        val items = listOf(currentItem) + tracks.map { track ->
                                                            QueueItem(track.id, track.name, track.ar.joinToString("/") { it.name }, track.al.picUrl)
                            }
                            playerManager.playQueue(items, 0, playContext = "intelligence")
                            _toastEvent.emit("已从《${firstSong.name}》开启心动模式")
                        }.onFailure {
                            _toastEvent.emit(it.toUserMessage(resourceProvider))
                        }
                    }
                }
            } else {
                viewModelScope.launch { _toastEvent.emit("请先播放一首歌曲") }
            }
        }
    }
}

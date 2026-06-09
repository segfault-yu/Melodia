package com.lin0721.linmusic.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.SettingsPreferences
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.ArtistInfo
import com.lin0721.linmusic.data.repository.LyricLine
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import com.lin0721.linmusic.player.QueueItem
import com.lin0721.linmusic.data.repository.SongWikiData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lin0721.linmusic.data.remote.api.CommentItem
import com.lin0721.linmusic.ui.components.ToastManager

class PlayerViewModel(
    private val context: Context,
    private val repository: MusicRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    // 监听 WiFi 下的播放音质设置
    val wifiQuality = settingsPreferences.wifiQuality.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "lossless"
    )

    // 监听移动网络下的播放音质设置
    val mobileQuality = settingsPreferences.mobileQuality.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "standard"
    )

    // 判断当前是否连接 WiFi
    fun isWifiConnected(): Boolean {
        return kotlin.runCatching {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        }.getOrDefault(false)
    }

    // 根据网络状态动态获取并组合成当前的活动播放音质 Flow
    val activeQuality: StateFlow<String> = settingsPreferences.wifiQuality
        .combine(settingsPreferences.mobileQuality) { wifi, mobile ->
            if (isWifiConnected()) wifi else mobile
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "standard"
        )

    // 更新当前环境的音质设置并重新加载当前歌曲播放
    fun updateQuality(quality: String) {
        viewModelScope.launch {
            if (isWifiConnected()) {
                settingsPreferences.saveWifiQuality(quality)
            } else {
                settingsPreferences.saveMobileQuality(quality)
            }
            playerManager.reloadCurrentTrack()
        }
    }

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _isUserScrolling = MutableStateFlow(false)
    val isUserScrolling: StateFlow<Boolean> = _isUserScrolling.asStateFlow()

    private val _songDetail = MutableStateFlow<Track?>(null)
    val songDetail: StateFlow<Track?> = _songDetail.asStateFlow()

    private val _songWiki = MutableStateFlow<SongWikiData?>(null)
    val songWiki: StateFlow<SongWikiData?> = _songWiki.asStateFlow()

    private val _similarArtists = MutableStateFlow<List<ArtistInfo>>(emptyList())
    val similarArtists: StateFlow<List<ArtistInfo>> = _similarArtists.asStateFlow()

    private val _isSimilarArtistsLoading = MutableStateFlow(false)
    val isSimilarArtistsLoading: StateFlow<Boolean> = _isSimilarArtistsLoading.asStateFlow()

    private val _artistDetail = MutableStateFlow<ArtistDetailInfo?>(null)
    val artistDetail: StateFlow<ArtistDetailInfo?> = _artistDetail.asStateFlow()

    // 歌手粉丝数量
    private val _artistFansCount = MutableStateFlow<Long?>(null)
    val artistFansCount: StateFlow<Long?> = _artistFansCount.asStateFlow()

    private val _artistAlbums = MutableStateFlow<List<ArtistAlbum>>(emptyList())
    val artistAlbums: StateFlow<List<ArtistAlbum>> = _artistAlbums.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    // 歌手是否已关注的 Flow
    private val _isArtistFollowed = MutableStateFlow(false)
    val isArtistFollowed: StateFlow<Boolean> = _isArtistFollowed.asStateFlow()

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Loading)
    val commentsState: StateFlow<CommentsState> = _commentsState.asStateFlow()

    private val likedSongIds = mutableSetOf<Long>()
    private var likedListLoaded = false

    private var currentSongId: Long = -1L

    init {
        loadLikedSongIds()
        observeTrackChanges()
        observePosition()
    }

    private fun loadLikedSongIds() {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first() ?: return@launch
            repository.getLikedSongIds(profile.uid).collect { result ->
                result.onSuccess { ids ->
                    likedSongIds.clear()
                    likedSongIds.addAll(ids)
                    likedListLoaded = true
                    if (currentSongId != -1L) {
                        _isLiked.value = currentSongId in likedSongIds
                    }
                }
            }
        }
    }

    fun toggleLike() {
        val songId = currentSongId
        if (songId == -1L) return

        val newLiked = !_isLiked.value
        _isLiked.value = newLiked

        viewModelScope.launch {
            repository.likeSong(songId, newLiked).collect { result ->
                result.onSuccess {
                    if (newLiked) likedSongIds.add(songId) else likedSongIds.remove(songId)
                }.onFailure {
                    // 回滚
                    _isLiked.value = !newLiked
                }
            }
        }
    }

    private fun observeTrackChanges() {
        viewModelScope.launch {
            playerManager.currentTrack
                .map { it?.mediaId?.toLongOrNull() ?: -1L }
                .distinctUntilChanged()
                .collectLatest { songId ->
                    if (songId != -1L && songId != currentSongId) {
                        currentSongId = songId
                        clearState()
                        _isLiked.value = songId in likedSongIds
                        loadLyrics(songId)
                        loadSongDetail(songId)
                        loadSongWiki(songId)
                        loadComments(songId)
                    }
                }
        }
    }

    private fun clearState() {
        _lyrics.value = emptyList()
        _currentLyricIndex.value = -1
        _songDetail.value = null
        _songWiki.value = null
        _similarArtists.value = emptyList()
        _artistDetail.value = null
        _artistFansCount.value = null
        _artistAlbums.value = emptyList()
        _isLiked.value = false
        _isArtistFollowed.value = false
        _commentsState.value = CommentsState.Loading
        _isUserScrolling.value = false
    }

    private fun observePosition() {
        viewModelScope.launch {
            playerManager.currentPosition.collectLatest { positionMs ->
                val lines = _lyrics.value
                if (lines.isEmpty()) return@collectLatest
                _currentLyricIndex.value = findLyricIndex(lines, positionMs)
            }
        }
    }

    private fun loadLyrics(songId: Long) {
        viewModelScope.launch {
            _isLyricsLoading.value = true
            repository.getLyrics(songId).collect { result ->
                result.onSuccess { lines ->
                    if (currentSongId == songId) _lyrics.value = lines
                }.onFailure {
                    if (currentSongId == songId) _lyrics.value = emptyList()
                }
            }
            _isLyricsLoading.value = false
        }
    }

    private fun loadSongDetail(songId: Long) {
        viewModelScope.launch {
            repository.getSongDetail(songId).collect { result ->
                result.onSuccess { track ->
                    if (currentSongId != songId) return@onSuccess
                    _songDetail.value = track
                    val primaryArtistId = track.ar.firstOrNull()?.id
                    if (primaryArtistId != null && primaryArtistId > 0) {
                        loadSimilarArtists(primaryArtistId, songId)
                        loadArtistDetail(primaryArtistId, songId)
                        loadArtistAlbums(primaryArtistId, songId)
                    }
                }
            }
        }
    }

    // 异步加载歌曲详情与音乐百科信息
    private fun loadSongWiki(songId: Long) {
        viewModelScope.launch {
            repository.getSongWiki(songId).collect { result ->
                if (currentSongId == songId) {
                    _songWiki.value = result.getOrNull()
                }
            }
        }
    }

    private fun loadSimilarArtists(artistId: Long, forSongId: Long) {
        viewModelScope.launch {
            _isSimilarArtistsLoading.value = true
            repository.getSimilarArtists(artistId).collect { result ->
                if (currentSongId != forSongId) return@collect
                result.onSuccess { artists ->
                    _similarArtists.value = artists
                }.onFailure {
                    _similarArtists.value = emptyList()
                }
            }
            _isSimilarArtistsLoading.value = false
        }
    }

    private fun loadArtistDetail(artistId: Long, forSongId: Long) {
        viewModelScope.launch {
            // 异步加载歌手粉丝数量作为每月听众数
            loadArtistFansCount(artistId, forSongId)
            // 异步加载当前用户是否关注了该歌手
            loadArtistFollowState(artistId, forSongId)
            repository.getArtistDetail(artistId).collect { result ->
                if (currentSongId != forSongId) return@collect
                result.onSuccess { detail ->
                    _artistDetail.value = detail
                }.onFailure {
                    _artistDetail.value = null
                }
            }
        }
    }

    // 异步加载歌手关注状态
    private fun loadArtistFollowState(artistId: Long, forSongId: Long) {
        viewModelScope.launch {
            repository.checkArtistFollowed(artistId).collect { result ->
                if (currentSongId != forSongId) return@collect
                result.onSuccess { followed ->
                    _isArtistFollowed.value = followed
                }.onFailure {
                    _isArtistFollowed.value = false
                }
            }
        }
    }

    // 异步获取歌手粉丝数
    private fun loadArtistFansCount(artistId: Long, forSongId: Long) {
        viewModelScope.launch {
            repository.getArtistFansCount(artistId).collect { result ->
                if (currentSongId != forSongId) return@collect
                result.onSuccess { count ->
                    _artistFansCount.value = count
                }.onFailure {
                    _artistFansCount.value = null
                }
            }
        }
    }

    private fun loadArtistAlbums(artistId: Long, forSongId: Long) {
        viewModelScope.launch {
            repository.getArtistAlbums(artistId).collect { result ->
                if (currentSongId != forSongId) return@collect
                result.onSuccess { albums ->
                    _artistAlbums.value = albums
                }.onFailure {
                    _artistAlbums.value = emptyList()
                }
            }
        }
    }

    private fun findLyricIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return result
    }

    private fun loadComments(songId: Long) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            repository.getComments(songId, limit = 15).collect { result ->
                if (currentSongId != songId) return@collect
                result.onSuccess { response ->
                    _commentsState.value = CommentsState.Success(
                        hotComments = response.hotComments,
                        comments = response.comments,
                        total = response.total
                    )
                }.onFailure { error ->
                    _commentsState.value = CommentsState.Error(error.message ?: "Unknown error")
                }
            }
        }
    }

    fun retryComments() {
        val songId = currentSongId
        if (songId != -1L) {
            loadComments(songId)
        }
    }

    // 切换歌手的关注状态
    fun toggleArtistFollow() {
        val songDetail = _songDetail.value ?: return
        val artistId = songDetail.ar.firstOrNull()?.id ?: return
        if (artistId <= 0) return

        val targetFollow = !_isArtistFollowed.value
        viewModelScope.launch {
            repository.subscribeArtist(artistId, targetFollow).collect { result ->
                result.onSuccess {
                    _isArtistFollowed.value = targetFollow
                }
            }
        }
    }

    fun setUserScrolling(scrolling: Boolean) {
        _isUserScrolling.value = scrolling
    }

    fun seekToTime(timeMs: Long) {
        playerManager.seekTo(timeMs)
    }

    val sleepTimerRemaining: StateFlow<Long> = playerManager.sleepTimerRemaining

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    // 开启相似歌曲漫游逻辑
    fun startSimilarSongsRoaming(songId: Long, currentTitle: String, currentArtist: String, currentCoverUrl: String) {
        viewModelScope.launch {
            repository.getSimilarSongs(songId).collect { result ->
                result.onSuccess { simiSongs ->
                    if (simiSongs.isNotEmpty()) {
                        val currentItem = QueueItem(songId, currentTitle, currentArtist, currentCoverUrl)
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
                        ToastManager.showToast("已开启相似歌曲漫游")
                    } else {
                        ToastManager.showToast("未找到相关相似歌曲")
                    }
                }.onFailure {
                    ToastManager.showToast("漫游开启失败")
                }
            }
        }
    }

    // 插播一首相似歌曲到下一首位置
    fun insertSimilarSongs(songId: Long) {
        viewModelScope.launch {
            repository.getSimilarSongs(songId).collect { result ->
                result.onSuccess { simiSongs ->
                    val firstSong = simiSongs.firstOrNull()
                    if (firstSong != null) {
                        val simiItem = QueueItem(
                            songId = firstSong.id,
                            title = firstSong.name,
                            artist = firstSong.ar.joinToString("/") { it.name },
                            coverUrl = firstSong.al.picUrl
                        )
                        playerManager.addToPlayNext(listOf(simiItem))
                        ToastManager.showToast("已成功插播相似歌曲《${firstSong.name}》到下一首")
                    } else {
                        ToastManager.showToast("暂无相似歌曲可插播")
                    }
                }.onFailure {
                    ToastManager.showToast("插播失败，请稍后重试")
                }
            }
        }
    }

    // 清空播放队列
    fun clearQueue() {
        playerManager.clearQueue()
    }
}

sealed interface CommentsState {
    object Loading : CommentsState
    data class Success(
        val hotComments: List<CommentItem>,
        val comments: List<CommentItem>,
        val total: Int
    ) : CommentsState
    data class Error(val message: String) : CommentsState
}

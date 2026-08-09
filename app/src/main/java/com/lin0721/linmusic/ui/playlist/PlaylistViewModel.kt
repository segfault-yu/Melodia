package com.lin0721.linmusic.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.core.api.PlaylistDetail
import com.lin0721.linmusic.core.api.Track
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import com.lin0721.linmusic.player.QueueItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.lin0721.linmusic.ui.player.CommentsState
import com.lin0721.linmusic.core.api.CommentItem
import com.lin0721.linmusic.core.api.DailySong


data class PlaylistCollectItem(
    val playlistId: Long,
    val playlistName: String,
    val coverUrl: String,
    val isInitiallyContains: Boolean,
    var isContains: Boolean
)

data class PlaylistCollectState(
    val songId: Long = -1L,
    val collectItems: List<PlaylistCollectItem> = emptyList(),
    val isLoading: Boolean = false
)

class PlaylistViewModel(
    private val repository: MusicRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private var allRecommendedTracks = listOf<Track>()
    private var currentRecIndex = 0

    private var isAlbumMode = false
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val userProfile = userPreferences.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _likedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedSongIds: StateFlow<Set<Long>> = _likedSongIds.asStateFlow()

    private val _collectState = MutableStateFlow(PlaylistCollectState())
    val collectState: StateFlow<PlaylistCollectState> = _collectState.asStateFlow()

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Loading)
    val commentsState: StateFlow<CommentsState> = _commentsState.asStateFlow()

    // 历史日推（每日推荐/听歌排行）浏览状态
    private val _historyRecommendState = MutableStateFlow(HistoryRecommendState())
    val historyRecommendState: StateFlow<HistoryRecommendState> = _historyRecommendState.asStateFlow()

    init {
        loadLikedSongIds()
    }

    fun loadLikedSongIds() {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first() ?: return@launch
            repository.getLikedSongIds(profile.uid).collect { result ->
                result.onSuccess { ids ->
                    _likedSongIds.value = ids.toSet()
                }
            }
        }
    }

    fun loadPlaylist(id: Long, isAlbum: Boolean = false) {
        isAlbumMode = isAlbum
        _uiState.value = PlaylistUiState.Loading
        allRecommendedTracks = emptyList()
        loadJob?.cancel() // 取消之前的加载任务
        if (id == -2L) {
            _historyRecommendState.update { it.copy(selectedDate = "最近一周") }
            loadJob = viewModelScope.launch {
                loadUserRecord(1)
            }
            return
        }
        if (id == -1L) {
            _historyRecommendState.update { it.copy(selectedDate = "今天") }
            loadJob = viewModelScope.launch {
                repository.getDailyRecommendSongs().collect { result ->
                    result.fold(
                        onSuccess = { dailySongs ->
                            val tracks = dailySongs.map { song ->
                                Track(
                                    id = song.id,
                                    name = song.name,
                                    ar = song.ar,
                                    al = song.al,
                                    fee = song.fee
                                )
                            }
                            val detail = PlaylistDetail(
                                id = -1L,
                                name = "每日推荐",
                                coverImgUrl = tracks.firstOrNull()?.al?.picUrl ?: "",
                                description = "为您量身定制的每日歌曲",
                                playCount = 0L,
                                tracks = tracks
                            )
                            _uiState.value = PlaylistUiState.Success(detail)
                            allRecommendedTracks = emptyList()
                        },
                        onFailure = { error ->
                            _uiState.value = PlaylistUiState.Error(error.message ?: "Unknown Error")
                        }
                    )
                }
            }
            return
        }
        loadJob = viewModelScope.launch {
            val flow = if (isAlbum) repository.getAlbumDetail(id) else repository.getPlaylistDetail(id)
            flow.collect { result ->
                result.fold(
                    onSuccess = { detail ->
                        _uiState.value = PlaylistUiState.Success(detail, isSubscribed = detail.subscribed)
                        val baseSong = detail.tracks.firstOrNull()

                        if (baseSong != null && !isAlbum) {
                            loadRecommendations(detail.id, baseSong.id, detail.tracks)
                        } else {
                            allRecommendedTracks = emptyList()
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = PlaylistUiState.Error(error.message ?: "Unknown Error")
                    }
                )
            }
        }
    }

    fun playSongInList(track: Track, allTracks: List<Track>) {
        val playlistName = (_uiState.value as? PlaylistUiState.Success)?.playlist?.name
        val queueItems = allTracks.map { t ->
            QueueItem(t.id, t.name, t.ar.joinToString { it.name }, t.al.picUrl)
        }
        val startIndex = allTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, startIndex, playlistName)
    }

    fun addTrackToPlayNext(track: Track) {
        val queueItem = QueueItem(track.id, track.name, track.ar.joinToString("/") { it.name }, track.al.picUrl)
        playerManager.addToPlayNext(listOf(queueItem))
        viewModelScope.launch { _toastEvent.emit("已添加至下一首播放") }
    }

    fun addTracksToPlayNext(tracks: List<Track>) {
        val queueItems = tracks.map { t ->
            QueueItem(t.id, t.name, t.ar.joinToString("/") { it.name }, t.al.picUrl)
        }
        playerManager.addToPlayNext(queueItems)
        viewModelScope.launch { _toastEvent.emit("已添加 ${queueItems.size} 首歌曲至播放队列") }
    }

    fun prepareCollectDialog(songId: Long) {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first() ?: return@launch
            _collectState.update { it.copy(songId = songId, isLoading = true, collectItems = emptyList()) }

            repository.getUserPlaylists(profile.uid).collect { result ->
                result.onSuccess { playlists ->
                    val myPlaylists = playlists.filter { it.userId == profile.uid }

                    val items = myPlaylists.map { playlist ->
                        async {
                            // 直接使用 _likedSongIds 判断
                            val isInitiallyContains = if (playlist.name.contains("喜欢的音乐") || playlist.id == profile.uid) {
                                _likedSongIds.value.contains(songId)
                            } else {
                                val detail = repository.getPlaylistDetail(playlist.id).firstOrNull()?.getOrNull()
                                detail?.tracks?.any { it.id == songId } ?: false
                            }
                            PlaylistCollectItem(
                                playlistId = playlist.id,
                                playlistName = playlist.name,
                                coverUrl = playlist.coverImgUrl,
                                isInitiallyContains = isInitiallyContains,
                                isContains = isInitiallyContains
                            )
                        }
                    }.awaitAll()

                    _collectState.update { it.copy(collectItems = items, isLoading = false) }
                }.onFailure {
                    _collectState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun savePlaylistCollection(songId: Long, items: List<PlaylistCollectItem>) {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first()
            items.forEach { item ->
                if (item.isContains != item.isInitiallyContains) {
                    val isLikedPlaylist = profile != null && (item.playlistName.contains("喜欢的音乐") || item.playlistId == profile.uid)
                    if (isLikedPlaylist) {
                        repository.likeSong(songId, item.isContains).collect { result ->
                            result.onSuccess {
                                val currentLiked = _likedSongIds.value.toMutableSet()
                                if (item.isContains) {
                                    currentLiked.add(songId)
                                } else {
                                    currentLiked.remove(songId)
                                }
                                _likedSongIds.value = currentLiked
                            }.onFailure { e ->
                                _toastEvent.emit("更新喜欢状态失败: ${e.message}")
                            }
                        }
                    } else {
                        val op = if (item.isContains) {
                            "add"
                        } else {
                            "del"
                        }
                        repository.manipulatePlaylistTracks(op, item.playlistId, songId).collect { result ->
                            result.onSuccess {
                                // 操作成功
                            }.onFailure { e ->
                                _toastEvent.emit("收藏至 [${item.playlistName}] 失败: ${e.message}")
                            }
                        }
                    }
                }
            }
            _toastEvent.emit("歌单收藏更新成功")
            val successState = _uiState.value as? PlaylistUiState.Success
            if (successState != null) {
                if (profile != null && successState.playlist.id == profile.uid) {
                    loadPlaylist(successState.playlist.id, isAlbumMode)
                }
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            repository.createPlaylist(name, privacy = 0).collect { result ->
                result.fold(
                    onSuccess = { playlist ->
                        repository.manipulatePlaylistTracks("add", playlist.id, songId).collect { addResult ->
                            addResult.fold(
                                onSuccess = {
                                    _toastEvent.emit("创建并加入歌单成功")
                                    prepareCollectDialog(songId)
                                },
                                onFailure = { e ->
                                    _toastEvent.emit("加入新建歌单失败: ${e.message}")
                                }
                            )
                        }
                    },
                    onFailure = { e ->
                        _toastEvent.emit("创建歌单失败: ${e.message}")
                    }
                )
            }
        }
    }

    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            _toastEvent.emit("登录成功，正在同步数据...")
            userPreferences.saveCookies(cookies)
            repository.getAccountInfo().collect { result ->
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
                        loadLikedSongIds()
                    }
                }
            }
        }
    }

    fun loadRecommendations(playlistId: Long, baseSongId: Long, existingTracks: List<Track>) {
        viewModelScope.launch {
            repository.getIntelligenceSongs(baseSongId, playlistId).collect { result ->
                result.onSuccess { recommendedList ->
                    val filteredList = recommendedList.filter { recTrack ->
                        existingTracks.none { it.id == recTrack.id }
                    }
                    allRecommendedTracks = filteredList
                    currentRecIndex = 0
                    updateCurrentRecommendations()
                }.onFailure { e ->
                    // 静默失败
                    allRecommendedTracks = emptyList()
                    setRecommendedSongs(emptyList())
                }
            }
        }
    }

    private fun setRecommendedSongs(songs: List<Track>) {
        _uiState.update { state ->
            if (state is PlaylistUiState.Success) state.copy(recommendedSongs = songs) else state
        }
    }

    private fun updateCurrentRecommendations() {
        if (allRecommendedTracks.isEmpty()) {
            setRecommendedSongs(emptyList())
            return
        }
        val size = allRecommendedTracks.size
        val start = currentRecIndex % size
        val list = mutableListOf<Track>()
        for (i in 0 until 5) {
            val idx = (start + i) % size
            val track = allRecommendedTracks[idx]
            if (!list.contains(track)) {
                list.add(track)
            }
            if (list.size >= size) break
        }
        setRecommendedSongs(list)
    }

    fun refreshRecommendations() {
        if (allRecommendedTracks.isNotEmpty()) {
            currentRecIndex = (currentRecIndex + 5) % allRecommendedTracks.size
            updateCurrentRecommendations()
        }
    }

    fun addRecommendSongToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.manipulatePlaylistTracks("add", playlistId, track.id).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("已添加到歌单")
                    allRecommendedTracks = allRecommendedTracks.filter { it.id != track.id }
                    updateCurrentRecommendations()

                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success) {
                            val updatedTracks = state.playlist.tracks.toMutableList().apply {
                                if (none { it.id == track.id }) {
                                    add(track)
                                }
                            }
                            state.copy(playlist = state.playlist.copy(tracks = updatedTracks))
                        } else state
                    }
                }.onFailure { e ->
                    _toastEvent.emit("添加失败: ${e.message}")
                }
            }
        }
    }

    fun toggleSubscribePlaylist() {
        val successState = _uiState.value as? PlaylistUiState.Success ?: return
        val playlistId = successState.playlist.id
        val targetSubscribe = !successState.isSubscribed
        viewModelScope.launch {
            repository.subscribePlaylist(playlistId, targetSubscribe).collect { result ->
                result.onSuccess {
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success) state.copy(isSubscribed = targetSubscribe) else state
                    }
                    _toastEvent.emit(if (targetSubscribe) "已收藏歌单" else "已取消收藏歌单")
                }.onFailure { e ->
                    _toastEvent.emit("操作失败: ${e.message}")
                }
            }
        }
    }

    fun loadPlaylistComments(playlistId: Long) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            val threadId = "A_PL_0_$playlistId"
            repository.getComments(threadId, limit = 20).collect { result ->
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

    fun likeComment(comment: CommentItem) {
        viewModelScope.launch {
            val profile = userProfile.value
            if (profile == null) {
                _toastEvent.emit("请先登录账号")
                return@launch
            }

            val currentState = _commentsState.value as? CommentsState.Success ?: return@launch
            
            val successState = _uiState.value as? PlaylistUiState.Success ?: return@launch
            val playlistId = successState.playlist.id
            val threadId = "A_PL_0_$playlistId"
            val targetLike = !comment.liked

            val updatedComments = currentState.comments.map {
                if (it.commentId == comment.commentId) {
                    it.copy(
                        liked = targetLike,
                        likedCount = it.likedCount + if (targetLike) 1 else -1
                    )
                } else it
            }
            val updatedHotComments = currentState.hotComments.map {
                if (it.commentId == comment.commentId) {
                    it.copy(
                        liked = targetLike,
                        likedCount = it.likedCount + if (targetLike) 1 else -1
                    )
                } else it
            }
            _commentsState.value = CommentsState.Success(
                hotComments = updatedHotComments,
                comments = updatedComments,
                total = currentState.total
            )

            repository.likeComment(threadId, comment.commentId, targetLike).collect { result ->
                result.onFailure { e ->
                    _commentsState.value = currentState
                    _toastEvent.emit("操作失败: ${e.message}")
                }
            }
        }
    }

    // 加载历史日推可用日期
    fun loadHistoryDates() {
        viewModelScope.launch {
            _historyRecommendState.update { it.copy(datesLoading = true) }
            repository.getHistoryRecommendDates().collect { result ->
                result.onSuccess { dates ->
                    _historyRecommendState.update { it.copy(dates = dates) }
                }.onFailure {
                    _toastEvent.emit("历史日推需要黑胶会员")
                }
                _historyRecommendState.update { it.copy(datesLoading = false) }
            }
        }
    }

    // 加载指定日期的历史日推歌曲
    fun loadHistoryDetail(date: String) {
        if (date == "weekly" || date == "all") {
            val type = if (date == "weekly") 1 else 0
            loadUserRecord(type)
            return
        }
        viewModelScope.launch {
            _historyRecommendState.update { it.copy(selectedDate = date, songsLoading = true) }
            repository.getHistoryRecommendDetail(date).collect { result ->
                result.onSuccess { songs ->
                    _historyRecommendState.update { it.copy(songs = songs) }
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == -1L) {
                            val newTracks = songs.map { song ->
                                Track(
                                    id = song.id,
                                    name = song.name,
                                    ar = song.ar,
                                    al = song.al,
                                    fee = song.fee
                                )
                            }
                            state.copy(playlist = state.playlist.copy(tracks = newTracks))
                        } else state
                    }
                }.onFailure {
                    _toastEvent.emit("加载失败：${it.message}")
                }
                _historyRecommendState.update { it.copy(songsLoading = false) }
            }
        }
    }

    fun loadUserRecord(type: Int) {
        viewModelScope.launch {
            val dateLabel = if (type == 1) "最近一周" else "所有时间"
            _historyRecommendState.update { it.copy(selectedDate = dateLabel, songsLoading = true) }
            val profile = userPreferences.userProfile.first()
            if (profile == null) {
                _toastEvent.emit("请先登录")
                _historyRecommendState.update { it.copy(songsLoading = false) }
                return@launch
            }
            repository.getUserRecord(profile.uid, type).collect { result ->
                result.fold(
                    onSuccess = { tracks ->
                        val detail = PlaylistDetail(
                            id = -2L,
                            name = "听歌排行的歌单",
                            coverImgUrl = tracks.firstOrNull()?.al?.picUrl ?: "",
                            description = "根据您的听歌记录统计",
                            playCount = 0L,
                            tracks = tracks
                        )
                        _uiState.value = PlaylistUiState.Success(detail)
                        _historyRecommendState.update { it.copy(songsLoading = false) }
                    },
                    onFailure = { error ->
                        _toastEvent.emit("获取听歌排行失败：${error.message}")
                        _historyRecommendState.update { it.copy(songsLoading = false) }
                    }
                )
            }
        }
    }

    fun playHistorySong(index: Int) {
        val songs = _historyRecommendState.value.songs
        if (songs.isEmpty()) return
        val queueItems = songs.map { song ->
            QueueItem(song.id, song.name, song.ar.joinToString { it.name }, song.al.picUrl)
        }
        playerManager.playQueue(queueItems, index.coerceIn(0, queueItems.size - 1), "历史日推")
    }

    fun toggleLikeSong(songId: Long, isLike: Boolean) {
        viewModelScope.launch {
            repository.likeSong(songId, isLike).collect { result ->
                result.fold(
                    onSuccess = {
                        _toastEvent.emit(if (isLike) "已添加到我喜欢的音乐" else "已从我喜欢的音乐中移除")
                        val currentLiked = _likedSongIds.value.toMutableSet()
                        if (isLike) {
                            currentLiked.add(songId)
                        } else {
                            currentLiked.remove(songId)
                        }
                        _likedSongIds.value = currentLiked

                        val successState = _uiState.value as? PlaylistUiState.Success
                        if (successState != null) {
                            val profile = userPreferences.userProfile.first()
                            if (profile != null && successState.playlist.id == profile.uid) {
                                loadPlaylist(successState.playlist.id, isAlbumMode)
                            }
                        }
                    },
                    onFailure = { e ->
                        _toastEvent.emit("操作失败: ${e.message}")
                    }
                )
            }
        }
    }
}

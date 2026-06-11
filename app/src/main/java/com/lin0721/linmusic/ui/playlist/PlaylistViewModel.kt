package com.lin0721.linmusic.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import com.lin0721.linmusic.player.QueueItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    private val _recommendedSongs = MutableStateFlow<List<Track>>(emptyList())
    val recommendedSongs: StateFlow<List<Track>> = _recommendedSongs.asStateFlow()

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
        _recommendedSongs.value = emptyList()
        allRecommendedTracks = emptyList()
        loadJob?.cancel() // 取消之前的加载任务，防止并发竞态冲突导致状态错乱
        loadJob = viewModelScope.launch {
            val flow = if (isAlbum) repository.getAlbumDetail(id) else repository.getPlaylistDetail(id)
            flow.collect { result ->
                result.fold(
                    onSuccess = { detail ->
                        _uiState.value = PlaylistUiState.Success(detail)
                        val baseSong = detail.tracks.firstOrNull()
                        if (baseSong != null && !isAlbum) {
                            loadRecommendations(detail.id, baseSong.id, detail.tracks)
                        } else {
                            _recommendedSongs.value = emptyList()
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

    fun prepareCollectDialog(songId: Long) {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first() ?: return@launch
            _collectState.update { it.copy(songId = songId, isLoading = true, collectItems = emptyList()) }

            repository.getUserPlaylists(profile.uid).collect { result ->
                result.onSuccess { playlists ->
                    val myPlaylists = playlists.filter { it.userId == profile.uid }

                    val items = myPlaylists.map { playlist ->
                        async {
                            // 喜欢歌单不需要额外请求详情，直接使用 _likedSongIds 判断
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
                                // 操作成功
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
            loadLikedSongIds()
            val successState = _uiState.value as? PlaylistUiState.Success
            if (successState != null) {
                loadPlaylist(successState.playlist.id, isAlbumMode)
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
                                    // 同时刷新喜欢状态和当前歌单（以防新建的是当前歌单，或者新建歌单影响了当前UI状态）
                                    loadLikedSongIds()
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
                    // 静默失败，不弹出 Toast，隐藏底部的推荐板块，提升用户体验
                    _recommendedSongs.value = emptyList()
                    allRecommendedTracks = emptyList()
                }
            }
        }
    }

    private fun updateCurrentRecommendations() {
        if (allRecommendedTracks.isEmpty()) {
            _recommendedSongs.value = emptyList()
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
        _recommendedSongs.value = list
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
                    
                    val currentState = _uiState.value
                    if (currentState is PlaylistUiState.Success) {
                        val updatedTracks = currentState.playlist.tracks.toMutableList().apply {
                            if (none { it.id == track.id }) {
                                add(track)
                            }
                        }
                        val updatedPlaylist = currentState.playlist.copy(tracks = updatedTracks)
                        _uiState.value = PlaylistUiState.Success(updatedPlaylist)
                    }
                }.onFailure { e ->
                    _toastEvent.emit("添加失败: ${e.message}")
                }
            }
        }
    }
}

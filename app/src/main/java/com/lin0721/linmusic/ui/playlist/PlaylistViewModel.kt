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

    fun loadPlaylist(id: Long) {
        _uiState.value = PlaylistUiState.Loading
        viewModelScope.launch {
            repository.getPlaylistDetail(id).collect { result ->
                result.fold(
                    onSuccess = { detail ->
                        _uiState.value = PlaylistUiState.Success(detail)
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
            items.forEach { item ->
                if (item.isContains != item.isInitiallyContains) {
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
            _toastEvent.emit("歌单收藏更新成功")
            loadLikedSongIds()
            val successState = _uiState.value as? PlaylistUiState.Success
            if (successState != null) {
                loadPlaylist(successState.playlist.id)
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
}

package com.lin0721.linmusic.feature.artist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.auth.SyncProfileAfterLoginUseCase
import com.lin0721.linmusic.core.songlike.LoadLikedSongIdsUseCase
import com.lin0721.linmusic.feature.artist.data.ArtistRepository
import com.lin0721.linmusic.feature.playlist.domain.CreatePlaylistAndAddSongUseCase
import com.lin0721.linmusic.core.userplaylist.UserPlaylistRepository
import com.lin0721.linmusic.core.songlike.SongLikeRepository
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArtistViewModel(
    private val createPlaylistAndAddSongUseCase: CreatePlaylistAndAddSongUseCase,
    private val syncProfileAfterLoginUseCase: SyncProfileAfterLoginUseCase,
    private val loadLikedSongIdsUseCase: LoadLikedSongIdsUseCase,
    private val userPlaylistRepository: UserPlaylistRepository,
    private val artistRepository: ArtistRepository,
    private val playlistRepository: PlaylistRepository,
    private val songLikeRepository: SongLikeRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val userProfile = userPreferences.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val blockedArtistIds = userPreferences.blockedArtistIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleBlockArtist(artistId: Long) {
        viewModelScope.launch {
            userPreferences.toggleBlockArtist(artistId)
            val isBlocked = userPreferences.blockedArtistIds.first().contains(artistId)
            val msg = if (isBlocked) "已屏蔽该艺人所有歌曲" else "已取消屏蔽该艺人所有歌曲"
            _toastEvent.emit(msg)
        }
    }

    private val _likedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedSongIds: StateFlow<Set<Long>> = _likedSongIds.asStateFlow()

    private val _collectState = MutableStateFlow(PlaylistCollectState())
    val collectState: StateFlow<PlaylistCollectState> = _collectState.asStateFlow()

    init {
        loadLikedSongIds()
    }

    fun loadLikedSongIds() {
        viewModelScope.launch {
            loadLikedSongIdsUseCase()?.let { _likedSongIds.value = it }
        }
    }

    fun loadArtistData(artistId: Long) {
        _uiState.value = ArtistUiState.Loading
        viewModelScope.launch {
            try {
                // 并行发起网络请求以提供极速的界面预加载
                val detailDeferred = async { artistRepository.getArtistDetail(artistId).first() }
                val fansDeferred = async { artistRepository.getArtistFansCount(artistId).first() }
                val followDeferred = async { artistRepository.checkArtistFollowed(artistId).first() }
                val topSongsDeferred = async { artistRepository.getArtistTopSongs(artistId).first() }
                val albumsDeferred = async { artistRepository.getArtistAlbums(artistId, limit = 50).first() }
                val similarDeferred = async { artistRepository.getSimilarArtists(artistId).first() }

                val detailResult = detailDeferred.await()
                val fansResult = fansDeferred.await()
                val followResult = followDeferred.await()
                val topSongsResult = topSongsDeferred.await()
                val albumsResult = albumsDeferred.await()
                val similarResult = similarDeferred.await()

                if (detailResult.isSuccess && topSongsResult.isSuccess) {
                    val detail = detailResult.getOrThrow()
                    val fans = fansResult.getOrDefault(0L)
                    val isFollowed = followResult.getOrDefault(false)
                    val topSongs = topSongsResult.getOrThrow()
                    val albums = albumsResult.getOrDefault(emptyList())
                    val similar = similarResult.getOrDefault(emptyList())

                    _uiState.value = ArtistUiState.Success(
                        artist = detail,
                        isFollowed = isFollowed,
                        fansCount = fans,
                        topSongs = topSongs,
                        albums = albums,
                        similarArtists = similar
                    )
                } else {
                    val err = (detailResult.exceptionOrNull() ?: topSongsResult.exceptionOrNull())
                        ?.toUserMessage(resourceProvider)
                        ?: resourceProvider.getString(com.lin0721.linmusic.R.string.app_error_biz_default)
                    _uiState.value = ArtistUiState.Error(err)
                }
            } catch (e: Exception) {
                _uiState.value = ArtistUiState.Error(e.toUserMessage(resourceProvider))
            }
        }
    }

    fun toggleFollow(artistId: Long) {
        val currentState = _uiState.value as? ArtistUiState.Success ?: return
        val targetSubscribe = !currentState.isFollowed
        viewModelScope.launch {
            artistRepository.subscribeArtist(artistId, targetSubscribe).collect { result ->
                result.onSuccess {
                    _uiState.value = currentState.copy(isFollowed = targetSubscribe)
                    val msg = if (targetSubscribe) "已关注歌手" else "已取消关注歌手"
                    _toastEvent.emit(msg)
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    fun playSongInList(track: Track, allTracks: List<Track>) {
        val artistName = (_uiState.value as? ArtistUiState.Success)?.artist?.name ?: "歌手热门歌曲"
        val queueItems = allTracks.map { t ->
            QueueItem(t.id, t.name, t.ar.joinToString { it.name }, t.al.picUrl)
        }
        val startIndex = allTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, startIndex, artistName)
    }

    fun prepareCollectDialog(songId: Long) {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first() ?: return@launch
            _collectState.update { it.copy(songId = songId, isLoading = true, collectItems = emptyList()) }

            userPlaylistRepository.getUserPlaylists(profile.uid).collect { result ->
                result.onSuccess { playlists ->
                    val myPlaylists = playlists.filter { it.userId == profile.uid }

                    val items = myPlaylists.map { playlist ->
                        async {
                            val isInitiallyContains = if (playlist.name.contains("喜欢的音乐") || playlist.id == profile.uid) {
                                _likedSongIds.value.contains(songId)
                            } else {
                                val detail = playlistRepository.getPlaylistDetail(playlist.id).firstOrNull()?.getOrNull()
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
                    _toastEvent.emit(it.toUserMessage(resourceProvider))
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
                        songLikeRepository.likeSong(songId, item.isContains).collect { result ->
                            result.onSuccess {
                                // 操作成功
                            }.onFailure { e ->
                                _toastEvent.emit(e.toUserMessage(resourceProvider))
                            }
                        }
                    } else {
                        val op = if (item.isContains) {
                            "add"
                        } else {
                            "del"
                        }
                        playlistRepository.manipulatePlaylistTracks(op, item.playlistId, songId).collect { result ->
                            result.onSuccess {
                                // 操作成功
                            }.onFailure { e ->
                                _toastEvent.emit(e.toUserMessage(resourceProvider))
                            }
                        }
                    }
                }
            }
            _toastEvent.emit("歌单收藏更新成功")
            loadLikedSongIds()
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            createPlaylistAndAddSongUseCase(name, songId).collect { result ->
                result.fold(
                    onSuccess = {
                        _toastEvent.emit("创建并加入歌单成功")
                        prepareCollectDialog(songId)
                        loadLikedSongIds()
                    },
                    onFailure = { e ->
                        _toastEvent.emit(e.toUserMessage(resourceProvider))
                    }
                )
            }
        }
    }

    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            if (syncProfileAfterLoginUseCase(cookies) == null) return@launch
            _toastEvent.emit("登录成功，正在同步数据...")
            // 同步红心列表并刷新歌手页，使关注态与红心态即时生效
            loadLikedSongIds()
            (uiState.value as? ArtistUiState.Success)?.let { successState ->
                loadArtistData(successState.artist.id)
            }
        }
    }
}

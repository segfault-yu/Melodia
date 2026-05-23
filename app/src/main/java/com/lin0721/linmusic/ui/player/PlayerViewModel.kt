package com.lin0721.linmusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.ArtistInfo
import com.lin0721.linmusic.data.repository.LyricLine
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import com.lin0721.linmusic.data.repository.SongWikiData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.lin0721.linmusic.data.remote.api.CommentItem

class PlayerViewModel(
    private val repository: MusicRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

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

    // 歌手粉丝数量 (用于每月听众数)
    private val _artistFansCount = MutableStateFlow<Long?>(null)
    val artistFansCount: StateFlow<Long?> = _artistFansCount.asStateFlow()

    private val _artistAlbums = MutableStateFlow<List<ArtistAlbum>>(emptyList())
    val artistAlbums: StateFlow<List<ArtistAlbum>> = _artistAlbums.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

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
                .map { it?.mediaMetadata?.extras?.getLong("songId") ?: -1L }
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
        _commentsState.value = CommentsState.Loading
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

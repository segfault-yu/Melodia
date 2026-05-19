package com.lin0721.linmusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.ArtistInfo
import com.lin0721.linmusic.data.repository.LyricLine
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: MusicRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _songDetail = MutableStateFlow<Track?>(null)
    val songDetail: StateFlow<Track?> = _songDetail.asStateFlow()

    private val _similarArtists = MutableStateFlow<List<ArtistInfo>>(emptyList())
    val similarArtists: StateFlow<List<ArtistInfo>> = _similarArtists.asStateFlow()

    private val _isSimilarArtistsLoading = MutableStateFlow(false)
    val isSimilarArtistsLoading: StateFlow<Boolean> = _isSimilarArtistsLoading.asStateFlow()

    private val _artistDetail = MutableStateFlow<ArtistDetailInfo?>(null)
    val artistDetail: StateFlow<ArtistDetailInfo?> = _artistDetail.asStateFlow()

    private val _artistAlbums = MutableStateFlow<List<ArtistAlbum>>(emptyList())
    val artistAlbums: StateFlow<List<ArtistAlbum>> = _artistAlbums.asStateFlow()

    private var currentSongId: Long = -1L

    init {
        observeTrackChanges()
        observePosition()
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
                        loadLyrics(songId)
                        loadSongDetail(songId)
                    }
                }
        }
    }

    private fun clearState() {
        _lyrics.value = emptyList()
        _currentLyricIndex.value = -1
        _songDetail.value = null
        _similarArtists.value = emptyList()
        _artistDetail.value = null
        _artistAlbums.value = emptyList()
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
}

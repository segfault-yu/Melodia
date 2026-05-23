package com.lin0721.linmusic.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import com.lin0721.linmusic.player.QueueItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val repository: MusicRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

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
}

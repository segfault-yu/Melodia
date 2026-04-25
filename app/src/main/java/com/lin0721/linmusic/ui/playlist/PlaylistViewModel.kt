package com.lin0721.linmusic.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val repository: MusicRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

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

    fun playSong(songId: Long, title: String, artist: String, coverUrl: String) {
        viewModelScope.launch {
            repository.getSongUrl(songId).collect { result ->
                result.fold(
                    onSuccess = { url ->
                        playerManager.playAudio(
                            url = url,
                            title = title,
                            artist = artist,
                            coverUrl = coverUrl
                        )
                    },
                    onFailure = {
                        // In a real app we could post errors to a SharedFlow.
                    }
                )
            }
        }
    }
}

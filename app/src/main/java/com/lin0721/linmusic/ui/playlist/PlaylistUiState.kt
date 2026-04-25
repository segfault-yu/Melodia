package com.lin0721.linmusic.ui.playlist

import com.lin0721.linmusic.data.remote.api.PlaylistDetail

sealed interface PlaylistUiState {
    object Loading : PlaylistUiState
    data class Success(val playlist: PlaylistDetail) : PlaylistUiState
    data class Error(val message: String) : PlaylistUiState
}

package com.lin0721.linmusic.feature.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.feature.search.data.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PlaylistCategoryViewModel(
    private val repository: SearchRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistCategoryUiState>(PlaylistCategoryUiState.Loading)
    val uiState: StateFlow<PlaylistCategoryUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var currentCategory: String? = null
    private var cursor: Long = 0
    private var job: Job? = null

    fun load(category: String) {
        if (currentCategory == category) return
        currentCategory = category
        cursor = 0
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = PlaylistCategoryUiState.Loading
            fetch(category, cursor = 0, isLoadMore = false)
        }
    }

    // 加载失败时的重试入口
    fun retry() {
        val category = currentCategory ?: return
        currentCategory = null
        load(category)
    }

    fun loadMore() {
        val category = currentCategory ?: return
        val current = _uiState.value
        if (current !is PlaylistCategoryUiState.Success || current.isLoadingMore || !current.hasMore) return

        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            fetch(category, cursor = cursor, isLoadMore = true)
        }
    }

    private suspend fun fetch(category: String, cursor: Long, isLoadMore: Boolean) {
        repository.getPlaylistsByCategory(category, cursor = cursor).firstOrNull()?.let { result ->
            result.onSuccess { page ->
                this.cursor = page.nextCursor
                val merged = if (isLoadMore) {
                    (_uiState.value as? PlaylistCategoryUiState.Success)?.playlists.orEmpty() + page.playlists
                } else {
                    page.playlists
                }
                _uiState.value = if (merged.isEmpty()) {
                    PlaylistCategoryUiState.Empty
                } else {
                    PlaylistCategoryUiState.Success(merged, page.hasMore, total = page.total, isLoadingMore = false)
                }
            }.onFailure { error ->
                val current = _uiState.value
                if (isLoadMore && current is PlaylistCategoryUiState.Success) {
                    _uiState.value = current.copy(isLoadingMore = false)
                    _toastEvent.emit(error.toUserMessage(resourceProvider))
                } else {
                    _uiState.value = PlaylistCategoryUiState.Error(error.toUserMessage(resourceProvider))
                }
            }
        }
    }
}

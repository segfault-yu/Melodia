package com.lin0721.linmusic.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.data.remote.api.SearchSong
import com.lin0721.linmusic.data.repository.HotSearch
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.data.repository.PlaylistTag
import com.lin0721.linmusic.player.PlayerManager
import com.lin0721.linmusic.player.QueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = true,
    val defaultKeyword: String = "搜索你想听的",
    val hotSearches: List<HotSearch> = emptyList(),
    val playlistTags: List<PlaylistTag> = emptyList(),
    val error: String? = null
)

class SearchViewModel(
    private val repository: MusicRepository,
    val playerManager: PlayerManager,
    userPreferences: UserPreferences
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userPreferences.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchSong>>(emptyList())
    val searchResults: StateFlow<List<SearchSong>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var currentOffset = 0

    init {
        loadDiscoveryData()
    }

    private fun loadDiscoveryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val keywordDeferred = async { repository.getDefaultSearchKeyword().firstOrNull() }
            val hotSearchDeferred = async { repository.getHotSearches().firstOrNull() }
            val tagsDeferred = async { repository.getPlaylistTags().firstOrNull() }

            val defaultKeyword = keywordDeferred.await()?.getOrNull() ?: "搜索你想听的"
            val hotSearches = hotSearchDeferred.await()?.getOrNull() ?: emptyList()
            val playlistTags = tagsDeferred.await()?.getOrNull() ?: emptyList()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                defaultKeyword = defaultKeyword,
                hotSearches = hotSearches,
                playlistTags = playlistTags
            )
        }
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _hasMore.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            performSearch(newQuery, isLoadMore = false)
        }
    }

    fun searchWithKeyword(keyword: String) {
        _isSearching.value = true
        _query.value = keyword
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(keyword, isLoadMore = false)
        }
    }

    fun activateSearch() {
        _isSearching.value = true
    }

    fun deactivateSearch() {
        _isSearching.value = false
        _query.value = ""
        _searchResults.value = emptyList()
        _hasMore.value = false
        searchJob?.cancel()
    }

    private suspend fun performSearch(keyword: String, isLoadMore: Boolean) {
        if (!isLoadMore) {
            currentOffset = 0
            _searchLoading.value = true
        }
        repository.searchSongs(keyword, offset = currentOffset).firstOrNull()?.let { result ->
            result.onSuccess { data ->
                if (isLoadMore) {
                    _searchResults.value = _searchResults.value + data.songs
                } else {
                    _searchResults.value = data.songs
                }
                currentOffset += data.songs.size
                _hasMore.value = data.hasMore
            }.onFailure { error ->
                _toastEvent.emit(error.message ?: "搜索失败")
            }
        }
        _searchLoading.value = false
    }

    fun loadMore() {
        val currentQuery = _query.value
        if (currentQuery.isBlank() || !_hasMore.value || _searchLoading.value) return
        viewModelScope.launch {
            performSearch(currentQuery, isLoadMore = true)
        }
    }

    fun playSong(song: SearchSong) {
        val results = _searchResults.value
        val queueItems = results.map { s ->
            QueueItem(s.id, s.name, s.ar.joinToString { it.name }, s.al.picUrl)
        }
        val startIndex = results.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, startIndex, "搜索")
    }
}

package com.lin0721.linmusic.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.PlayerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LibraryItemType {
    PLAYLIST, ARTIST, ALBUM
}

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val type: LibraryItemType,
    val isPinned: Boolean = false,
    val updateTime: Long = 0,
    val trackCount: Int = 0,
    val playCount: Long = 0,
    val isLikedSongs: Boolean = false
)

enum class LibraryFilter {
    ALL, PLAYLIST, ALBUM, ARTIST
}

enum class LibrarySortOrder {
    RECENTLY_PLAYED, CREATE_TIME, NAME
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val allItems: List<LibraryItem> = emptyList(),
    val filteredItems: List<LibraryItem> = emptyList(),
    val artistCount: Int = 0,
    val playlistCount: Int = 0,
    val albumCount: Int = 0,
    val error: String? = null,
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_PLAYED,
    val searchQuery: String = ""
)

class LibraryViewModel(
    private val repository: MusicRepository,
    private val userPreferences: UserPreferences,
    val playerManager: PlayerManager,
    private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)
    private val _pinnedIds = MutableStateFlow<Set<String>>(getPinnedIdsFromPrefs())

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.userProfile.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
                if (profile != null) {
                    loadLibraryData()
                } else {
                    _uiState.update {
                        it.copy(
                            allItems = emptyList(),
                            filteredItems = emptyList(),
                            artistCount = 0,
                            playlistCount = 0,
                            albumCount = 0
                        )
                    }
                }
            }
        }
    }

    private fun getPinnedIdsFromPrefs(): Set<String> {
        return sharedPrefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()
    }

    private fun savePinnedIdsToPrefs(ids: Set<String>) {
        sharedPrefs.edit().putStringSet("pinned_ids", ids).apply()
    }

    fun loadLibraryData() {
        val profile = _uiState.value.userProfile ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 1. 并行获取歌单
                val playlistsDeferred = async {
                    repository.getUserPlaylists(profile.uid).firstOrNull()?.getOrNull() ?: emptyList()
                }
                
                // 2. 并行获取歌手
                val artistsDeferred = async {
                    repository.getFavoriteArtists().firstOrNull()?.getOrNull() ?: emptyList()
                }
                
                // 3. 并行获取专辑
                val albumsDeferred = async {
                    repository.getCollectedAlbums().firstOrNull()?.getOrNull() ?: emptyList()
                }
                
                // 4. 并行获取用户收藏统计数
                val subcountDeferred = async {
                    repository.getUserSubcount().firstOrNull()?.getOrNull()
                }

                val playlists = playlistsDeferred.await()
                val artists = artistsDeferred.await()
                val albums = albumsDeferred.await()
                val subcount = subcountDeferred.await()

                // 数据归一化 (Mapping)
                val mappedPlaylists = playlists.mapIndexed { index, playlist ->
                    LibraryItem(
                        id = playlist.id.toString(),
                        title = playlist.name,
                        subtitle = "歌单 · ${playlist.creator?.nickname ?: ""}",
                        coverUrl = playlist.coverImgUrl,
                        type = LibraryItemType.PLAYLIST,
                        updateTime = playlist.updateTime,
                        trackCount = playlist.trackCount,
                        playCount = playlist.playCount,
                        isLikedSongs = index == 0 && playlist.creator?.userId == profile.uid
                    )
                }

                val mappedArtists = artists.map { artist ->
                    LibraryItem(
                        id = artist.id.toString(),
                        title = artist.name,
                        subtitle = "歌手",
                        coverUrl = artist.avatarUrl,
                        type = LibraryItemType.ARTIST,
                        updateTime = 0
                    )
                }

                val mappedAlbums = albums.map { album ->
                    LibraryItem(
                        id = album.id.toString(),
                        title = album.name,
                        subtitle = "专辑 · ${album.artists.joinToString(" • ") { it.name }}",
                        coverUrl = album.picUrl,
                        type = LibraryItemType.ALBUM,
                        updateTime = album.subTime
                    )
                }

                val combinedItems = mappedPlaylists + mappedArtists + mappedAlbums

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allItems = combinedItems,
                        artistCount = subcount?.artistCount ?: artists.size,
                        playlistCount = subcount?.playlistCount ?: playlists.size,
                        albumCount = subcount?.albumCount ?: albums.size
                    )
                }
                
                applyFilterAndSort()
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "获取音乐库数据失败")
                }
            }
        }
    }

    private fun applyFilterAndSort() {
        val state = _uiState.value
        val pinned = _pinnedIds.value
        
        var list = state.allItems.map { item ->
            item.copy(isPinned = pinned.contains(item.id))
        }
        
        if (state.searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(state.searchQuery, ignoreCase = true) ||
                it.subtitle.contains(state.searchQuery, ignoreCase = true)
            }
        }
        
        list = when (state.selectedFilter) {
            LibraryFilter.ALL -> list
            LibraryFilter.PLAYLIST -> list.filter { it.type == LibraryItemType.PLAYLIST }
            LibraryFilter.ALBUM -> list.filter { it.type == LibraryItemType.ALBUM }
            LibraryFilter.ARTIST -> list.filter { it.type == LibraryItemType.ARTIST }
        }
        
        val pinnedItems = list.filter { it.isPinned }
        val unpinnedItems = list.filter { !it.isPinned }
        
        val sortedUnpinned = when (state.sortOrder) {
            LibrarySortOrder.RECENTLY_PLAYED -> {
                unpinnedItems.sortedByDescending { it.updateTime }
            }
            LibrarySortOrder.CREATE_TIME -> {
                unpinnedItems.sortedByDescending { it.updateTime }
            }
            LibrarySortOrder.NAME -> {
                unpinnedItems.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
        }
        
        val finalList = pinnedItems + sortedUnpinned
        
        _uiState.update {
            it.copy(filteredItems = finalList)
        }
    }

    fun togglePin(itemId: String) {
        val currentPinned = _pinnedIds.value.toMutableSet()
        if (currentPinned.contains(itemId)) {
            currentPinned.remove(itemId)
        } else {
            currentPinned.add(itemId)
        }
        _pinnedIds.value = currentPinned
        savePinnedIdsToPrefs(currentPinned)
        applyFilterAndSort()
    }

    fun createPlaylist(name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.createPlaylist(name).collect { result ->
                result.onSuccess {
                    loadLibraryData()
                    onSuccess()
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.localizedMessage ?: "创建歌单失败") }
                }
            }
        }
    }

    fun updateFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilterAndSort()
    }

    fun updateSortOrder(order: LibrarySortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        applyFilterAndSort()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
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
                        loadLibraryData()
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserProfile()
        }
    }
}

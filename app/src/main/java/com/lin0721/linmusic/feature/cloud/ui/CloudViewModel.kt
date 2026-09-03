package com.lin0721.linmusic.feature.cloud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.feature.cloud.data.CloudRepository
import com.lin0721.linmusic.feature.cloud.domain.CloudSong
import com.lin0721.linmusic.feature.search.data.SearchRepository
import com.lin0721.linmusic.feature.search.domain.SearchResultItem
import com.lin0721.linmusic.feature.search.domain.SearchType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "CloudViewModel"
private const val PAGE_SIZE = 30
private const val MATCH_SEARCH_DEBOUNCE_MS = 400L

class CloudViewModel(
    private val cloudRepository: CloudRepository,
    private val searchRepository: SearchRepository,
    private val userPreferences: UserPreferences,
    val playerManager: PlayerManager,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<CloudUiState>(CloudUiState.Loading)
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    private val _overlay = MutableStateFlow<CloudOverlay>(CloudOverlay.Hidden)
    val overlay: StateFlow<CloudOverlay> = _overlay.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var matchSearchJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CloudUiState.Loading
            cloudRepository.getCloudSongs(limit = PAGE_SIZE, offset = 0).first()
                .onSuccess { page ->
                    _uiState.value = CloudUiState.Success(
                        songs = page.songs,
                        quota = page.quota,
                        hasMore = page.hasMore
                    )
                }
                .onFailure { e ->
                    _uiState.value = CloudUiState.Error(e.toUserMessage(resourceProvider))
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value as? CloudUiState.Success ?: return
        if (!state.hasMore || state.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            cloudRepository.getCloudSongs(limit = PAGE_SIZE, offset = state.songs.size).first()
                .onSuccess { page ->
                    _uiState.value = state.copy(
                        songs = state.songs + page.songs,
                        quota = page.quota,
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "加载更多失败", e)
                    _uiState.value = state.copy(isLoadingMore = false)
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
        }
    }

    // 以当前已加载的云盘歌曲为播放队列，从点击项开始播
    fun playSong(song: CloudSong) {
        val state = _uiState.value as? CloudUiState.Success ?: return
        val queueItems = state.songs.map { item ->
            QueueItem(
                songId = item.songId,
                title = item.name,
                artist = item.artist,
                coverUrl = item.coverUrl.orEmpty()
            )
        }
        val startIndex = state.songs.indexOfFirst { it.songId == song.songId }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, startIndex, "我的云盘")
    }

    // ======================= 三点菜单 / 删除 =======================

    fun openOptions(song: CloudSong) {
        _overlay.value = CloudOverlay.Options(song)
    }

    fun dismissOverlay() {
        matchSearchJob?.cancel()
        _overlay.value = CloudOverlay.Hidden
    }

    fun requestDelete() {
        val song = (_overlay.value as? CloudOverlay.Options)?.song ?: return
        _overlay.value = CloudOverlay.DeleteConfirm(song)
    }

    fun confirmDelete() {
        val current = _overlay.value as? CloudOverlay.DeleteConfirm ?: return
        if (current.isDeleting) return

        viewModelScope.launch {
            _overlay.value = current.copy(isDeleting = true)
            cloudRepository.deleteCloudSong(current.song.songId).first()
                .onSuccess {
                    removeSongFromList(current.song)
                    _overlay.value = CloudOverlay.Hidden
                    _toastEvent.emit("已删除")
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "删除云盘歌曲失败", e)
                    _overlay.value = current.copy(isDeleting = false)
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
        }
    }

    private fun removeSongFromList(song: CloudSong) {
        val state = _uiState.value as? CloudUiState.Success ?: return
        _uiState.value = state.copy(
            songs = state.songs.filterNot { it.songId == song.songId },
            quota = state.quota.copy(
                usedBytes = (state.quota.usedBytes - song.fileSizeBytes).coerceAtLeast(0),
                totalCount = (state.quota.totalCount - 1).coerceAtLeast(0)
            )
        )
    }

    // ======================= 重新匹配 =======================

    fun openMatch() {
        val song = (_overlay.value as? CloudOverlay.Options)?.song ?: return
        _overlay.value = CloudOverlay.Match(song = song)
    }

    fun updateMatchQuery(query: String) {
        val current = _overlay.value as? CloudOverlay.Match ?: return
        _overlay.value = current.copy(query = query, confirmTarget = null)
        matchSearchJob?.cancel()

        if (query.isBlank()) {
            _overlay.value = (_overlay.value as? CloudOverlay.Match)?.copy(results = emptyList(), isSearching = false) ?: return
            return
        }

        matchSearchJob = viewModelScope.launch {
            delay(MATCH_SEARCH_DEBOUNCE_MS)
            _overlay.value = (_overlay.value as? CloudOverlay.Match)?.copy(isSearching = true) ?: return@launch
            searchRepository.search(query, SearchType.SONG, offset = 0, limit = 20).first()
                .onSuccess { result ->
                    val tracks = result.items.filterIsInstance<SearchResultItem.SongItem>().map { it.track }
                    _overlay.value = (_overlay.value as? CloudOverlay.Match)?.copy(
                        results = tracks,
                        isSearching = false
                    ) ?: return@onSuccess
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "重新匹配搜索失败", e)
                    _overlay.value = (_overlay.value as? CloudOverlay.Match)?.copy(isSearching = false) ?: return@onFailure
                }
        }
    }

    fun selectMatchCandidate(track: Track) {
        val current = _overlay.value as? CloudOverlay.Match ?: return
        _overlay.value = current.copy(confirmTarget = track)
    }

    fun cancelMatchConfirm() {
        val current = _overlay.value as? CloudOverlay.Match ?: return
        _overlay.value = current.copy(confirmTarget = null)
    }

    fun confirmMatch() {
        val current = _overlay.value as? CloudOverlay.Match ?: return
        val target = current.confirmTarget ?: return
        if (current.isMatching) return

        viewModelScope.launch {
            val uid = userPreferences.userProfile.first()?.uid ?: return@launch
            _overlay.value = current.copy(isMatching = true)
            cloudRepository.matchCloudSong(userId = uid, songId = current.song.songId, adjustSongId = target.id).first()
                .onSuccess {
                    updateSongAfterMatch(current.song, target)
                    _overlay.value = CloudOverlay.Hidden
                    _toastEvent.emit("匹配成功")
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "重新匹配失败", e)
                    _overlay.value = current.copy(isMatching = false)
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
        }
    }

    // 匹配接口不回显新数据，直接用选中的候选曲目信息更新本地这一行
    private fun updateSongAfterMatch(song: CloudSong, target: Track) {
        val state = _uiState.value as? CloudUiState.Success ?: return
        _uiState.value = state.copy(
            songs = state.songs.map {
                if (it.songId == song.songId) {
                    it.copy(
                        name = target.name,
                        artist = target.ar.joinToString(" / ") { artist -> artist.name },
                        coverUrl = target.al.picUrl.ifBlank { null },
                        isUnmatched = false
                    )
                } else {
                    it
                }
            }
        )
    }
}

package com.lin0721.linmusic.feature.newworks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.feature.newworks.data.NewWorksRepository
import com.lin0721.linmusic.feature.newworks.domain.NewWorksRelease
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private const val TAG = "NewWorksViewModel"

class NewWorksViewModel(
    private val repository: NewWorksRepository,
    val playerManager: PlayerManager,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewWorksUiState>(NewWorksUiState.Loading)
    val uiState: StateFlow<NewWorksUiState> = _uiState.asStateFlow()

    private var cursor: Long = System.currentTimeMillis()
    private var loaded = false
    private var job: Job? = null

    // 首页音乐 tab 每次挂载都会调，避免切来切去重复请求
    fun loadIfNeeded() {
        if (!loaded) load()
    }

    fun load() {
        loaded = true
        cursor = System.currentTimeMillis()
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = NewWorksUiState.Loading

            val mvsResult = fetch("新 MV") { repository.getMvs() }
            val releasesResult = fetch("新发布") { repository.getReleases(cursor, firstRequest = true) }

            releasesResult.onSuccess { page ->
                cursor = page.nextCursor
                _uiState.value = NewWorksUiState.Success(
                    mvs = mvsResult.getOrDefault(emptyList()),
                    releases = page.items.distinctByReleaseKey(),
                    hasMore = page.hasMore,
                    isLoadingMore = false
                )
            }.onFailure { error ->
                _uiState.value = NewWorksUiState.Error(error.toUserMessage(resourceProvider))
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current !is NewWorksUiState.Success || current.isLoadingMore || !current.hasMore) return

        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            fetch("新发布-翻页") { repository.getReleases(cursor, firstRequest = false) }
                .onSuccess { page ->
                    cursor = page.nextCursor
                    val latest = _uiState.value
                    if (latest is NewWorksUiState.Success) {
                        _uiState.value = latest.copy(
                            releases = (latest.releases + page.items).distinctByReleaseKey(),
                            hasMore = page.hasMore,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure {
                    val latest = _uiState.value
                    if (latest is NewWorksUiState.Success) {
                        _uiState.value = latest.copy(isLoadingMore = false)
                    }
                }
        }
    }

    // 单曲发布点击即播；专辑发布交给调用方跳转，这里不处理
    fun playRelease(release: NewWorksRelease) {
        val state = _uiState.value as? NewWorksUiState.Success ?: return
        val singles = state.releases.filter { !it.isAlbum }
        if (singles.isEmpty()) return

        val queueItems = singles.map { QueueItem(it.id, it.title, it.artistName, it.coverUrl) }
        val startIndex = singles.indexOfFirst { it.id == release.id }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, startIndex, "音乐新作")
    }

    // 单类请求的异常兜底，避免一类失败连累另一类结果丢失
    private suspend fun <T> fetch(label: String, block: suspend () -> Flow<Result<T>>): Result<T> =
        runCatching { block().firstOrNull() ?: Result.failure(IllegalStateException("$label 无响应")) }
            .getOrElse { e -> Result.failure(e) }
            .onFailure { AppLogger.w(TAG, "$label 加载失败", it) }

    // 真机实测：同一专辑会在单页内重复出现，翻页游标边界重叠时也会跨页重复——
    // 网格用 (isAlbum, id) 做 key，重复项会直接让 LazyVerticalGrid 崩溃，必须去重
    private fun List<NewWorksRelease>.distinctByReleaseKey(): List<NewWorksRelease> =
        distinctBy { it.isAlbum to it.id }
}

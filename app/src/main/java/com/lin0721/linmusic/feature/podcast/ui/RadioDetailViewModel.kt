package com.lin0721.linmusic.feature.podcast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.feature.podcast.data.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 服务端一页给 30 条
private const val PAGE_SIZE = 30

// 电台详情页 ViewModel
class RadioDetailViewModel(
    private val podcastRepository: PodcastRepository,
    private val playerManager: PlayerManager,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<RadioDetailUiState>(RadioDetailUiState.Loading)
    val uiState: StateFlow<RadioDetailUiState> = _uiState.asStateFlow()

    private var currentRadioId: Long = 0

    fun load(radioId: Long) {
        // 同一个电台重复进入不必重拉
        if (currentRadioId == radioId && _uiState.value is RadioDetailUiState.Success) return
        currentRadioId = radioId
        _uiState.value = RadioDetailUiState.Loading

        viewModelScope.launch {
            try {
                val detailDeferred = async { podcastRepository.getRadioDetail(radioId).first() }
                val programsDeferred = async {
                    runCatching { podcastRepository.getRadioPrograms(radioId).first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val detailResult = detailDeferred.await()
                val detail = detailResult.getOrNull()

                if (detail == null) {
                    _uiState.value = RadioDetailUiState.Error(
                        detailResult.exceptionOrNull()?.toUserMessage(resourceProvider)
                            ?: resourceProvider.getString(R.string.app_error_biz_default)
                    )
                    return@launch
                }

                val programs = programsDeferred.await().getOrDefault(emptyList())
                _uiState.value = RadioDetailUiState.Success(
                    detail = detail,
                    programs = programs,
                    hasMore = programs.size >= PAGE_SIZE
                )
            } catch (e: Exception) {
                _uiState.value = RadioDetailUiState.Error(e.toUserMessage(resourceProvider))
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value as? RadioDetailUiState.Success ?: return
        if (!current.hasMore || current.isLoadingMore) return

        _uiState.value = current.copy(isLoadingMore = true)

        viewModelScope.launch {
            val more = runCatching {
                podcastRepository.getRadioPrograms(currentRadioId, offset = current.programs.size).first()
            }.getOrNull()?.getOrNull()

            val latest = _uiState.value as? RadioDetailUiState.Success ?: return@launch
            _uiState.value = if (more.isNullOrEmpty()) {
                // 追加失败或已到底，停止继续翻页但保留已有内容
                latest.copy(isLoadingMore = false, hasMore = false)
            } else {
                latest.copy(
                    programs = latest.programs + more,
                    isLoadingMore = false,
                    hasMore = more.size >= PAGE_SIZE
                )
            }
        }
    }

    // 从指定一期起播，整个已加载列表作为队列
    fun playAt(index: Int) {
        val current = _uiState.value as? RadioDetailUiState.Success ?: return
        if (current.programs.isEmpty()) return

        val queue = current.programs.map { program ->
            QueueItem(
                songId = program.songId,
                title = program.name,
                artist = listOfNotNull(
                    program.radioName.takeIf { it.isNotBlank() },
                    program.djName.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                coverUrl = program.coverUrl
            )
        }
        playerManager.playQueue(queue, index.coerceIn(queue.indices), playContext = "radio_${current.detail.id}")
    }
}

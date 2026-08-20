package com.lin0721.linmusic.core.player

import android.content.Context
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.player.data.PlaybackRepository
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "PlayerManager"

// 播放门面：对外暴露播放状态与控制入口，队列、控制器、进度、持久化等职责交由协作者承担
class PlayerManager(
    private val context: Context,
    private val playbackPreferences: PlaybackPreferences,
    private val repository: PlaybackRepository,
    private val settingsPreferences: SettingsPreferences
) : Player.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    private val controllerHolder = MediaControllerHolder(context)
    private val playbackQueue = PlaybackQueue()
    private val progress = PlaybackProgressTracker(scope, controllerHolder)
    private val stateStore = PlaybackStateStore(scope, playbackPreferences)
    private val coverPreloader = TrackCoverPreloader(context)
    private val sleepTimer = SleepTimer(scope) { pause() }
    private val roaming = SimilarRoamingController(scope, repository, settingsPreferences, playbackQueue, stateStore)
    private val networkGuard = PlaybackNetworkGuard(
        context = context,
        scope = scope,
        settingsPreferences = settingsPreferences,
        isPlaying = { _isPlaying.value },
        onPauseRequested = { pause() }
    )

    val currentPosition: StateFlow<Long> = progress.currentPosition
    val duration: StateFlow<Long> = progress.duration
    val positionUpdateInterval: StateFlow<Long> = progress.updateInterval
    val sleepTimerRemaining: StateFlow<Long> = sleepTimer.remaining
    val playContext: StateFlow<String?> = playbackQueue.playContext
    val currentIndex: StateFlow<Int> = playbackQueue.currentIndex
    val playMode: StateFlow<PlayMode> = playbackQueue.playMode
    val queue: StateFlow<List<QueueItem>> = playbackQueue.items

    private var activePlayJob: Job? = null
    private var consecutiveErrors = 0

    init {
        networkGuard.register()

        scope.launch {
            playbackQueue.setPlayMode(stateStore.loadPlayMode())
            // 恢复队列
            val qs = stateStore.loadQueueState()
            playbackQueue.restore(qs.queue, qs.currentIndex, qs.playContext)
        }

        // 监听 playMode 同步，newValue != oldValue 判定防止死循环写入
        scope.launch {
            stateStore.playModeChanges.collectLatest { newMode ->
                if (playbackQueue.playMode.value != newMode) {
                    applyMode(newMode, playbackQueue.currentItem())
                }
            }
        }

        progress.start(
            isPlaying = { _isPlaying.value },
            onTick = { positionMs ->
                val dur = progress.duration.value
                val songId = _currentTrack.value?.mediaId?.toLongOrNull() ?: -1L
                if (dur > 0L && songId != -1L) {
                    roaming.onProgressTick(songId, dur - positionMs)
                }
            }
        )
    }

    fun setPositionUpdateInterval(intervalMs: Long) {
        progress.setUpdateInterval(intervalMs)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimer.start(minutes)
    }

    suspend fun initController() {
        if (controllerHolder.isConnected) return

        val lastTrack = stateStore.loadLastTrack()
        if (lastTrack != null && _currentTrack.value == null) {
            _currentTrack.value = lastTrack.mediaItem
            progress.setPosition(lastTrack.positionMs)
        }

        controllerHolder.connect(this) { mediaController ->
            if (mediaController.currentMediaItem != null) {
                _currentTrack.value = mediaController.currentMediaItem
                _isPlaying.value = mediaController.isPlaying
                progress.setPosition(mediaController.currentPosition)
                progress.setDuration(mediaController.duration)
            }

            mediaController.repeatMode = if (playbackQueue.playMode.value == PlayMode.SINGLE_LOOP)
                Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    // 设置队列并从指定位置开始播放
    fun playQueue(items: List<QueueItem>, startIndex: Int, playContext: String? = null) {
        if (items.isEmpty()) return

        if (playContext == SimilarRoamingController.CONTEXT_ROAMING) {
            roaming.prepare()
        }

        playbackQueue.setPlayContext(playContext)
        playbackQueue.replaceAll(items, startIndex)
        consecutiveErrors = 0
        saveQueueState()
        fetchUrlAndPlay(playbackQueue.currentIndex.value)
    }

    // 单曲播放（向后兼容，创建 1 项队列）
    fun playAudio(songId: Long, url: String, title: String, artist: String, coverUrl: String, startPosition: Long = 0, playContext: String? = null) {
        val item = QueueItem(songId, title, artist, coverUrl)
        playbackQueue.replaceWithSingle(item)
        playbackQueue.setPlayContext(playContext)
        consecutiveErrors = 0

        controllerHolder.playItem(item.toMediaItem(url, playContext), playbackQueue.playMode.value, startPosition)
    }

    // 批量插播歌曲到“下一首”播放位置
    fun addToPlayNext(items: List<QueueItem>) {
        if (items.isEmpty()) return
        if (playbackQueue.isEmpty) {
            playQueue(items, 0)
            return
        }

        playbackQueue.insertNext(items)
        saveQueueState()
    }

    fun playNext() {
        if (playbackQueue.isEmpty) return
        consecutiveErrors = 0
        fetchUrlAndPlay(playbackQueue.nextIndex())
    }

    fun playPrevious() {
        if (playbackQueue.isEmpty) return
        val position = controllerHolder.currentPosition
        if (position > 3000) {
            seekTo(0)
            return
        }
        consecutiveErrors = 0
        fetchUrlAndPlay(playbackQueue.previousIndex())
    }

    fun playAtIndex(index: Int) {
        if (index < 0 || index >= playbackQueue.size) return
        consecutiveErrors = 0
        fetchUrlAndPlay(index)
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= playbackQueue.size || playbackQueue.size <= 1) return
        val replayIndex = playbackQueue.removeAt(index)
        if (replayIndex >= 0) fetchUrlAndPlay(replayIndex)
        saveQueueState()
    }

    fun moveInQueue(from: Int, to: Int) {
        if (!playbackQueue.move(from, to)) return
        saveQueueState()
    }

    fun toggleShuffle() {
        val currentItem = playbackQueue.currentItem()
        when (playbackQueue.playMode.value) {
            PlayMode.SHUFFLE -> applyMode(PlayMode.LIST_LOOP, currentItem)
            else -> applyMode(PlayMode.SHUFFLE, currentItem)
        }
    }

    fun toggleRepeat() {
        val currentItem = playbackQueue.currentItem()
        when (playbackQueue.playMode.value) {
            PlayMode.LIST_LOOP -> applyMode(PlayMode.SINGLE_LOOP, currentItem)
            PlayMode.SINGLE_LOOP -> applyMode(PlayMode.LIST_LOOP, currentItem)
            PlayMode.SHUFFLE -> applyMode(PlayMode.SINGLE_LOOP, currentItem)
        }
    }

    fun rotatePlayMode() {
        val currentItem = playbackQueue.currentItem()
        val nextMode = when (playbackQueue.playMode.value) {
            PlayMode.LIST_LOOP -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SINGLE_LOOP
            PlayMode.SINGLE_LOOP -> PlayMode.LIST_LOOP
        }
        applyMode(nextMode, currentItem)
    }

    private fun applyMode(newMode: PlayMode, currentItem: QueueItem?) {
        playbackQueue.applyMode(newMode, currentItem)
        controllerHolder.setRepeatMode(newMode)
        stateStore.savePlayMode(newMode)
        saveQueueState()
    }

    fun pause() {
        controllerHolder.pause()
        saveState()
    }

    fun resume() {
        controllerHolder.play()
    }

    fun seekTo(positionMs: Long) {
        controllerHolder.seekTo(positionMs)
        progress.setPosition(positionMs)
    }

    fun togglePlayPause() {
        val item = _currentTrack.value ?: return
        if (!_isPlaying.value && item.localConfiguration == null) {
            // 重启后队列为空，从恢复的 track 元数据重建 1 项队列
            if (playbackQueue.isEmpty) {
                val songId = item.mediaId.toLongOrNull() ?: return
                val qi = QueueItem(
                    songId = songId,
                    title = item.mediaMetadata.title?.toString() ?: "",
                    artist = item.mediaMetadata.artist?.toString() ?: "",
                    coverUrl = item.mediaMetadata.artworkUri?.toString() ?: ""
                )
                playbackQueue.replaceWithSingle(qi)
            }
            fetchUrlAndPlay(playbackQueue.currentIndex.value, progress.currentPosition.value)
            return
        }
        if (_isPlaying.value) pause() else resume()
    }

    fun saveState() {
        val item = _currentTrack.value ?: return
        val songId = item.mediaId.toLongOrNull() ?: -1L
        if (songId == -1L) return

        stateStore.savePlaybackState {
            PlaybackState(
                songId = songId,
                title = item.mediaMetadata.title?.toString() ?: "",
                artist = item.mediaMetadata.artist?.toString() ?: "",
                coverUrl = item.mediaMetadata.artworkUri?.toString() ?: "",
                lastPositionMs = controllerHolder.currentPositionOrNull ?: progress.currentPosition.value
            )
        }
    }

    // 清空播放队列中除当前播放歌曲外的其他歌曲，重置播放状态并同步本地持久化状态
    fun clearQueue() {
        val currentTrackItem = playbackQueue.keepOnlyCurrent()

        if (currentTrackItem != null) {
            _isPlaying.value = false
            progress.setPosition(0L)

            controllerHolder.pauseAndRewind()

            saveQueueState()
            stateStore.savePlaybackState {
                PlaybackState(
                    songId = currentTrackItem.songId,
                    title = currentTrackItem.title,
                    artist = currentTrackItem.artist,
                    coverUrl = currentTrackItem.coverUrl,
                    lastPositionMs = 0L
                )
            }
        } else {
            _currentTrack.value = null
            _isPlaying.value = false
            progress.setPosition(0L)
            progress.setDuration(0L)

            controllerHolder.stopAndClear()

            saveQueueState()
            stateStore.savePlaybackState {
                PlaybackState(
                    songId = -1L,
                    title = "",
                    artist = "",
                    coverUrl = "",
                    lastPositionMs = 0L
                )
            }
        }
    }

    private fun saveQueueState() {
        stateStore.saveQueue(playbackQueue)
    }

    private fun fetchUrlAndPlay(index: Int, startPosition: Long = 0) {
        val item = playbackQueue.itemAt(index) ?: return

        activePlayJob?.cancel()
        roaming.cancel()

        activePlayJob = scope.launch {
            if (networkGuard.blockPlaybackOnMobile()) return@launch

            playbackQueue.setCurrentIndex(index)
            saveQueueState()

            // 立即重置当前进度与时长，避免上一首歌曲的数据在加载新歌期间残留导致进度条闪烁
            progress.resetTo(startPosition)

            roaming.prefetchOnPlay(item.songId, index)

            repository.getSongUrl(item.songId).collect { result ->
                result.onSuccess { url ->
                    val mediaItem = item.toMediaItem(url, playbackQueue.playContext.value)
                    controllerHolder.playItem(mediaItem, playbackQueue.playMode.value, startPosition)
                }.onFailure { throwable ->
                    AppLogger.w(TAG, "获取播放URL失败 songId=${item.songId} index=$index", throwable)
                    skipToNextOnError(index)
                }
            }
        }
    }

    fun release() {
        networkGuard.unregister()
        sleepTimer.cancel()
        activePlayJob?.cancel()
        roaming.cancel()
    }

    private fun skipToNextOnError(failedIndex: Int) {
        consecutiveErrors++
        if (consecutiveErrors >= 3 || playbackQueue.size <= 1) {
            AppLogger.e(TAG, "连续 $consecutiveErrors 次播放失败，放弃自动切歌 failedIndex=$failedIndex queueSize=${playbackQueue.size}")
            scope.launch {
                Toast.makeText(context, "无法获取该歌曲的播放链接", Toast.LENGTH_SHORT).show()
            }
            return
        }
        fetchUrlAndPlay(playbackQueue.nextIndexFrom(failedIndex))
    }

    // 重新加载当前歌曲（用于切换音质时立即生效）
    fun reloadCurrentTrack() {
        val index = playbackQueue.currentIndex.value
        if (index >= 0 && index < playbackQueue.size) {
            val currentPos = controllerHolder.currentPosition
            fetchUrlAndPlay(index, currentPos)
        }
    }

    // 关闭漫游并还原备份的队列数据
    fun disableRoaming() {
        roaming.disable()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) consecutiveErrors = 0
        if (!isPlaying) saveState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        AppLogger.i(TAG, "切歌: songId=${mediaItem?.mediaId} reason=${transitionReasonName(reason)}")
        _currentTrack.value = mediaItem
        playbackQueue.setPlayContext(mediaItem?.mediaMetadata?.extras?.getString("playContext"))
        if (mediaItem != null) {
            // 切歌过渡时，应当立即将当前进度重置，防止读取上一首残留位置或缓冲位置
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                progress.setPosition(0L)
            }
            progress.updateDurationFromController()
            saveState()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        AppLogger.i(TAG, "播放状态变化: ${playbackStateName(playbackState)}")
        if (playbackState == Player.STATE_READY) {
            progress.updateDurationFromController()
            playbackQueue.nextItemByMode()?.let { coverPreloader.preload(it.coverUrl) }
        }
        // 单曲循环由 ExoPlayer REPEAT_MODE_ONE 处理，不会到达 STATE_ENDED
        if (playbackState == Player.STATE_ENDED && playbackQueue.playMode.value != PlayMode.SINGLE_LOOP) {
            playNext()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        AppLogger.e(TAG, "播放器报错 errorCode=${error.errorCodeName} songId=${_currentTrack.value?.mediaId}", error)
        scope.launch {
            Toast.makeText(context, "当前歌曲无法播放，已自动跳过", Toast.LENGTH_SHORT).show()
        }
        skipToNextOnError(playbackQueue.currentIndex.value)
    }

    private fun playbackStateName(state: Int) = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($state)"
    }

    private fun transitionReasonName(reason: Int) = when (reason) {
        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
        else -> "UNKNOWN($reason)"
    }
}

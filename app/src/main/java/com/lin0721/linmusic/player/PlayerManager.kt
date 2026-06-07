package com.lin0721.linmusic.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lin0721.linmusic.data.local.PlaybackPreferences
import com.lin0721.linmusic.data.local.PlaybackState
import com.lin0721.linmusic.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayerManager(
    private val context: Context,
    private val playbackPreferences: PlaybackPreferences,
    private val repository: MusicRepository
) : Player.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _positionUpdateInterval = MutableStateFlow(1000L)
    val positionUpdateInterval: StateFlow<Long> = _positionUpdateInterval.asStateFlow()

    fun setPositionUpdateInterval(intervalMs: Long) {
        _positionUpdateInterval.value = intervalMs
    }

    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0L
            return
        }
        _sleepTimerRemaining.value = minutes * 60 * 1000L
        sleepTimerJob = scope.launch {
            while (_sleepTimerRemaining.value > 0L) {
                delay(1000L)
                val currentVal = _sleepTimerRemaining.value
                val nextVal = currentVal - 1000L
                if (nextVal <= 0L) {
                    _sleepTimerRemaining.value = 0L
                    pause()
                    break
                } else {
                    _sleepTimerRemaining.value = nextVal
                }
            }
        }
    }

    private val _playContext = MutableStateFlow<String?>(null)
    val playContext: StateFlow<String?> = _playContext.asStateFlow()

    // 队列管理
    private var originalQueue: List<QueueItem> = emptyList()
    private var playQueue: List<QueueItem> = emptyList()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.LIST_LOOP)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private var activePlayJob: Job? = null
    private var consecutiveErrors = 0

    init {
        scope.launch {
            _playMode.value = playbackPreferences.playMode.first()
            // 恢复队列
            val qs = playbackPreferences.queueState.first()
            if (qs.queue.isNotEmpty()) {
                originalQueue = qs.queue
                playQueue = qs.queue
                _currentIndex.value = qs.currentIndex.coerceIn(0, qs.queue.size - 1)
                _queue.value = playQueue
                _playContext.value = qs.playContext
            }
        }
        scope.launch {
            while (true) {
                // 只有当播放器处于就绪播放状态时，才去轮询更新当前进度，防止在切歌缓冲时读到残留或脏进度值
                val isReady = controller?.playbackState == Player.STATE_READY
                if (_isPlaying.value && isReady) {
                    _currentPosition.value = controller?.currentPosition ?: 0L
                }
                delay(_positionUpdateInterval.value)
            }
        }
    }

    suspend fun initController() {
        if (controller != null) return

        val lastState = playbackPreferences.playbackState.first()
        if (lastState.songId != -1L && _currentTrack.value == null) {
            val bundle = Bundle().apply { putLong("songId", lastState.songId) }
            val metadata = MediaMetadata.Builder()
                .setTitle(lastState.title)
                .setArtist(lastState.artist)
                .setArtworkUri(Uri.parse(lastState.coverUrl))
                .setExtras(bundle)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(lastState.songId.toString())
                .setMediaMetadata(metadata)
                .build()

            _currentTrack.value = mediaItem
            _currentPosition.value = lastState.lastPositionMs
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MelodiaPlaybackService::class.java)
        )

        controller = suspendCancellableCoroutine { continuation ->
            val factory = MediaController.Builder(context, sessionToken).buildAsync()
            factory.addListener(
                {
                    val mediaController = factory.get()
                    mediaController.addListener(this@PlayerManager)

                    if (mediaController.currentMediaItem != null) {
                        _currentTrack.value = mediaController.currentMediaItem
                        _isPlaying.value = mediaController.isPlaying
                        _currentPosition.value = mediaController.currentPosition
                        _duration.value = mediaController.duration
                    }

                    mediaController.repeatMode = if (_playMode.value == PlayMode.SINGLE_LOOP)
                        Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                    continuation.resume(mediaController)
                },
                ContextCompat.getMainExecutor(context)
            )

            continuation.invokeOnCancellation {
                factory.cancel(true)
            }
        }
    }

    // 设置队列并从指定位置开始播放
    fun playQueue(items: List<QueueItem>, startIndex: Int, playContext: String? = null) {
        if (items.isEmpty()) return
        originalQueue = items
        _playContext.value = playContext

        if (_playMode.value == PlayMode.SHUFFLE) {
            playQueue = shufflePreservingCurrent(items, startIndex)
            _currentIndex.value = 0
        } else {
            playQueue = items
            _currentIndex.value = startIndex.coerceIn(0, items.size - 1)
        }
        _queue.value = playQueue
        consecutiveErrors = 0
        saveQueueState()
        fetchUrlAndPlay(_currentIndex.value)
    }

    // 单曲播放（向后兼容，创建 1 项队列）
    fun playAudio(songId: Long, url: String, title: String, artist: String, coverUrl: String, startPosition: Long = 0, playContext: String? = null) {
        val item = QueueItem(songId, title, artist, coverUrl)
        originalQueue = listOf(item)
        playQueue = listOf(item)
        _currentIndex.value = 0
        _queue.value = playQueue
        _playContext.value = playContext
        consecutiveErrors = 0

        val mediaItem = item.toMediaItem(url, playContext)
        controller?.apply {
            repeatMode = if (_playMode.value == PlayMode.SINGLE_LOOP)
                Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            setMediaItem(mediaItem)
            prepare()
            if (startPosition > 0) seekTo(startPosition)
            play()
        }
    }

    fun playNext() {
        if (playQueue.isEmpty()) return
        consecutiveErrors = 0
        val nextIndex = (_currentIndex.value + 1) % playQueue.size
        fetchUrlAndPlay(nextIndex)
    }

    fun playPrevious() {
        if (playQueue.isEmpty()) return
        val position = controller?.currentPosition ?: 0L
        if (position > 3000) {
            seekTo(0)
            return
        }
        consecutiveErrors = 0
        val prevIndex = if (_currentIndex.value - 1 < 0) playQueue.size - 1 else _currentIndex.value - 1
        fetchUrlAndPlay(prevIndex)
    }

    fun playAtIndex(index: Int) {
        if (index < 0 || index >= playQueue.size) return
        consecutiveErrors = 0
        fetchUrlAndPlay(index)
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= playQueue.size || playQueue.size <= 1) return
        val removedItem = playQueue[index]
        val mutablePlay = playQueue.toMutableList()
        mutablePlay.removeAt(index)
        playQueue = mutablePlay

        val mutableOrig = originalQueue.toMutableList()
        val origIdx = mutableOrig.indexOfFirst { it.songId == removedItem.songId }
        if (origIdx >= 0) mutableOrig.removeAt(origIdx)
        originalQueue = mutableOrig

        when {
            index < _currentIndex.value -> _currentIndex.value -= 1
            index == _currentIndex.value -> {
                val newIdx = _currentIndex.value.coerceAtMost(playQueue.size - 1)
                _currentIndex.value = newIdx
                fetchUrlAndPlay(newIdx)
            }
        }
        _queue.value = playQueue
        saveQueueState()
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from == to) return
        if (from < 0 || from >= playQueue.size || to < 0 || to >= playQueue.size) return
        val mutable = playQueue.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        playQueue = mutable
        originalQueue = mutable.toList()

        val cur = _currentIndex.value
        _currentIndex.value = when (cur) {
            from -> to
            in (minOf(from, to)..maxOf(from, to)) ->
                if (from < to) cur - 1 else cur + 1
            else -> cur
        }
        _queue.value = playQueue
        saveQueueState()
    }

    fun toggleShuffle() {
        val currentItem = playQueue.getOrNull(_currentIndex.value)
        when (_playMode.value) {
            PlayMode.SHUFFLE -> applyMode(PlayMode.LIST_LOOP, currentItem)
            else -> applyMode(PlayMode.SHUFFLE, currentItem)
        }
    }

    fun toggleRepeat() {
        val currentItem = playQueue.getOrNull(_currentIndex.value)
        when (_playMode.value) {
            PlayMode.LIST_LOOP -> applyMode(PlayMode.SINGLE_LOOP, currentItem)
            PlayMode.SINGLE_LOOP -> applyMode(PlayMode.LIST_LOOP, currentItem)
            PlayMode.SHUFFLE -> applyMode(PlayMode.SINGLE_LOOP, currentItem)
        }
    }

    private fun applyMode(newMode: PlayMode, currentItem: QueueItem?) {
        _playMode.value = newMode

        when (newMode) {
            PlayMode.SINGLE_LOOP -> {
                controller?.repeatMode = Player.REPEAT_MODE_ONE
            }
            PlayMode.SHUFFLE -> {
                controller?.repeatMode = Player.REPEAT_MODE_OFF
                if (currentItem != null && originalQueue.isNotEmpty()) {
                    val origIdx = originalQueue.indexOfFirst { it.songId == currentItem.songId }.coerceAtLeast(0)
                    playQueue = shufflePreservingCurrent(originalQueue, origIdx)
                    _currentIndex.value = 0
                    _queue.value = playQueue
                }
            }
            PlayMode.LIST_LOOP -> {
                controller?.repeatMode = Player.REPEAT_MODE_OFF
                if (currentItem != null && originalQueue.isNotEmpty()) {
                    playQueue = originalQueue
                    _currentIndex.value = originalQueue.indexOfFirst { it.songId == currentItem.songId }.coerceAtLeast(0)
                    _queue.value = playQueue
                }
            }
        }
        scope.launch { playbackPreferences.savePlayMode(newMode) }
        saveQueueState()
    }

    fun pause() {
        controller?.pause()
        saveState()
    }

    fun resume() {
        controller?.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun togglePlayPause() {
        val item = _currentTrack.value ?: return
        if (!_isPlaying.value && item.localConfiguration == null) {
            // 重启后队列为空，从恢复的 track 元数据重建 1 项队列
            if (playQueue.isEmpty()) {
                val songId = item.mediaId.toLongOrNull() ?: return
                val qi = QueueItem(
                    songId = songId,
                    title = item.mediaMetadata.title?.toString() ?: "",
                    artist = item.mediaMetadata.artist?.toString() ?: "",
                    coverUrl = item.mediaMetadata.artworkUri?.toString() ?: ""
                )
                originalQueue = listOf(qi)
                playQueue = listOf(qi)
                _currentIndex.value = 0
                _queue.value = playQueue
            }
            fetchUrlAndPlay(_currentIndex.value, _currentPosition.value)
            return
        }
        if (_isPlaying.value) pause() else resume()
    }

    fun saveState() {
        val item = _currentTrack.value ?: return
        val songId = item.mediaId.toLongOrNull() ?: -1L
        if (songId == -1L) return

        scope.launch {
            playbackPreferences.savePlaybackState(
                PlaybackState(
                    songId = songId,
                    title = item.mediaMetadata.title?.toString() ?: "",
                    artist = item.mediaMetadata.artist?.toString() ?: "",
                    coverUrl = item.mediaMetadata.artworkUri?.toString() ?: "",
                    lastPositionMs = controller?.currentPosition ?: _currentPosition.value
                )
            )
        }
    }

    private fun saveQueueState() {
        scope.launch {
            playbackPreferences.saveQueueState(originalQueue, _currentIndex.value, _playContext.value)
        }
    }

    private fun fetchUrlAndPlay(index: Int, startPosition: Long = 0) {
        if (index < 0 || index >= playQueue.size) return
        val item = playQueue[index]
        _currentIndex.value = index
        saveQueueState()

        // 立即重置当前进度与时长，避免上一首歌曲的数据在加载新歌期间残留导致进度条闪烁
        _currentPosition.value = startPosition
        _duration.value = 0L

        activePlayJob?.cancel()
        activePlayJob = scope.launch {
            repository.getSongUrl(item.songId).collect { result ->
                result.onSuccess { url ->
                    val mediaItem = item.toMediaItem(url, _playContext.value)
                    controller?.apply {
                        repeatMode = if (_playMode.value == PlayMode.SINGLE_LOOP)
                            Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                        setMediaItem(mediaItem)
                        prepare()
                        if (startPosition > 0) seekTo(startPosition)
                        play()
                    }
                }.onFailure {
                    skipToNextOnError(index)
                }
            }
        }
    }

    private fun skipToNextOnError(failedIndex: Int) {
        consecutiveErrors++
        if (consecutiveErrors >= 3 || playQueue.size <= 1) {
            scope.launch {
                android.widget.Toast.makeText(context, "无法获取该歌曲的播放链接", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }
        val nextIndex = (failedIndex + 1) % playQueue.size
        fetchUrlAndPlay(nextIndex)
    }

    private fun shufflePreservingCurrent(items: List<QueueItem>, currentIndex: Int): List<QueueItem> {
        if (items.size <= 1) return items
        val mutable = items.toMutableList()
        val safeIdx = currentIndex.coerceIn(0, mutable.size - 1)
        val current = mutable.removeAt(safeIdx)
        for (i in mutable.size - 1 downTo 1) {
            val j = (0..i).random()
            mutable[i] = mutable[j].also { mutable[j] = mutable[i] }
        }
        mutable.add(0, current)
        return mutable
    }

    // 重新加载当前歌曲（用于切换音质时立即生效）
    fun reloadCurrentTrack() {
        val index = _currentIndex.value
        if (index >= 0 && index < playQueue.size) {
            val currentPos = controller?.currentPosition ?: 0L
            fetchUrlAndPlay(index, currentPos)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) consecutiveErrors = 0
        if (!isPlaying) saveState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _currentTrack.value = mediaItem
        _playContext.value = mediaItem?.mediaMetadata?.extras?.getString("playContext")
        if (mediaItem != null) {
            // 切歌过渡时，应当立即将当前进度重置，防止读取上一首残留位置或缓冲位置
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                _currentPosition.value = 0L
            }
            val dur = controller?.duration ?: 0L
            _duration.value = if (dur > 0L) dur else 0L
            saveState()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            val dur = controller?.duration ?: 0L
            _duration.value = if (dur > 0L) dur else 0L
        }
        // 单曲循环由 ExoPlayer REPEAT_MODE_ONE 处理，不会到达 STATE_ENDED
        if (playbackState == Player.STATE_ENDED && _playMode.value != PlayMode.SINGLE_LOOP) {
            playNext()
        }
    }

    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        super.onPlayerError(error)
        scope.launch {
            android.widget.Toast.makeText(context, "当前歌曲无法播放，已自动跳过", android.widget.Toast.LENGTH_SHORT).show()
        }
        skipToNextOnError(_currentIndex.value)
    }
}

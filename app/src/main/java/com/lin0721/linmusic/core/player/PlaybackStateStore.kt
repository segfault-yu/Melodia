package com.lin0721.linmusic.core.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 上次退出时的曲目与进度
data class RestoredTrack(val mediaItem: MediaItem, val positionMs: Long)

// 播放状态持久化：封装队列、播放模式与当前曲目的读写，写入统一异步执行
class PlaybackStateStore(
    private val scope: CoroutineScope,
    private val preferences: PlaybackPreferences
) {

    val playModeChanges: Flow<PlayMode> get() = preferences.playMode

    suspend fun loadPlayMode(): PlayMode = preferences.playMode.first()

    suspend fun loadQueueState(): QueueState = preferences.queueState.first()

    // 从持久化状态重建上次播放的曲目，无历史记录时返回 null
    suspend fun loadLastTrack(): RestoredTrack? {
        val lastState = preferences.playbackState.first()
        if (lastState.songId == -1L) return null

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

        return RestoredTrack(mediaItem, lastState.lastPositionMs)
    }

    fun savePlayMode(mode: PlayMode) {
        scope.launch { preferences.savePlayMode(mode) }
    }

    // 队列内容在协程内读取，保证落盘的是写入时刻的最新队列
    fun saveQueue(queue: PlaybackQueue) {
        scope.launch(Dispatchers.Default) {
            preferences.saveQueueState(queue.original, queue.currentIndex.value, queue.playContext.value)
        }
    }

    // 状态在协程内构建，保证落盘的是写入时刻的播放进度
    fun savePlaybackState(provider: () -> PlaybackState) {
        scope.launch { preferences.savePlaybackState(provider()) }
    }
}

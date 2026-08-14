package com.lin0721.linmusic.core.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// 持有与 MelodiaPlaybackService 的 MediaController 连接，收敛所有播放器指令下发
class MediaControllerHolder(private val context: Context) {

    private var controller: MediaController? = null

    val isConnected: Boolean get() = controller != null

    val currentPosition: Long get() = controller?.currentPosition ?: 0L

    // 控制器未连接时返回 null，供调用方回退到本地缓存进度
    val currentPositionOrNull: Long? get() = controller?.currentPosition

    val duration: Long get() = controller?.duration ?: 0L

    val playbackState: Int get() = controller?.playbackState ?: Player.STATE_IDLE

    // 建立连接，onReady 在控制器就绪后、字段赋值前回调，用于同步初始状态
    suspend fun connect(listener: Player.Listener, onReady: (MediaController) -> Unit) {
        if (controller != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MelodiaPlaybackService::class.java)
        )

        controller = suspendCancellableCoroutine { continuation ->
            val factory = MediaController.Builder(context, sessionToken).buildAsync()
            factory.addListener(
                {
                    val mediaController = factory.get()
                    mediaController.addListener(listener)
                    onReady(mediaController)
                    continuation.resume(mediaController)
                },
                ContextCompat.getMainExecutor(context)
            )

            continuation.invokeOnCancellation {
                factory.cancel(true)
            }
        }
    }

    fun setRepeatMode(mode: PlayMode) {
        controller?.repeatMode = if (mode == PlayMode.SINGLE_LOOP)
            Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // 装载并起播单个曲目，起播位置大于 0 时先定位再播放
    fun playItem(mediaItem: MediaItem, mode: PlayMode, startPosition: Long) {
        controller?.apply {
            repeatMode = if (mode == PlayMode.SINGLE_LOOP)
                Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            setMediaItem(mediaItem)
            prepare()
            if (startPosition > 0) seekTo(startPosition)
            play()
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun pauseAndRewind() {
        controller?.pause()
        controller?.seekTo(0)
    }

    fun stopAndClear() {
        controller?.stop()
        controller?.clearMediaItems()
    }
}

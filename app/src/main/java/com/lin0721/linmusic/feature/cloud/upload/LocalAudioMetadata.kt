package com.lin0721.linmusic.feature.cloud.upload

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

data class LocalAudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?
)

// 读取本地音频文件的 ID3 标签，读不到时对应字段为 null，由调用方决定兜底文案
// （参考实现 cloud.js 里读不到时分别兜底为文件名/"未知专辑"/"未知艺术家"）
fun extractLocalAudioMetadata(context: Context, uri: Uri): LocalAudioMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        LocalAudioMetadata(
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.ifBlank { null },
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()?.ifBlank { null },
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()?.ifBlank { null }
        )
    } catch (e: Exception) {
        LocalAudioMetadata(title = null, artist = null, album = null)
    } finally {
        retriever.release()
    }
}

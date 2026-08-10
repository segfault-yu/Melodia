package com.lin0721.linmusic.core.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.Serializable

enum class PlayMode { LIST_LOOP, SINGLE_LOOP, SHUFFLE }

@Serializable
data class QueueItem(
    val songId: Long,
    val title: String,
    val artist: String,
    val coverUrl: String
) {
    fun toMediaItem(url: String, playContext: String? = null): MediaItem {
        val bundle = Bundle().apply {
            putLong("songId", songId)
            if (playContext != null) putString("playContext", playContext)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(coverUrl))
            .setExtras(bundle)
            .build()
        return MediaItem.Builder()
            .setUri(url)
            .setMediaId(songId.toString())
            .setMediaMetadata(metadata)
            .build()
    }
}

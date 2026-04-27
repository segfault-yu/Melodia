package com.lin0721.linmusic.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "playback_prefs")

data class PlaybackState(
    val songId: Long = -1,
    val title: String = "",
    val artist: String = "",
    val coverUrl: String = "",
    val lastPositionMs: Long = 0
)

class PlaybackPreferences(private val context: Context) {

    companion object {
        private val KEY_SONG_ID = longPreferencesKey("last_song_id")
        private val KEY_TITLE = stringPreferencesKey("last_song_title")
        private val KEY_ARTIST = stringPreferencesKey("last_song_artist")
        private val KEY_COVER = stringPreferencesKey("last_song_cover")
        private val KEY_POSITION = longPreferencesKey("last_position_ms")
    }

    val playbackState: Flow<PlaybackState> = context.dataStore.data.map { prefs ->
        PlaybackState(
            songId = prefs[KEY_SONG_ID] ?: -1,
            title = prefs[KEY_TITLE] ?: "",
            artist = prefs[KEY_ARTIST] ?: "",
            coverUrl = prefs[KEY_COVER] ?: "",
            lastPositionMs = prefs[KEY_POSITION] ?: 0
        )
    }

    suspend fun savePlaybackState(state: PlaybackState) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SONG_ID] = state.songId
            prefs[KEY_TITLE] = state.title
            prefs[KEY_ARTIST] = state.artist
            prefs[KEY_COVER] = state.coverUrl
            prefs[KEY_POSITION] = state.lastPositionMs
        }
    }
}

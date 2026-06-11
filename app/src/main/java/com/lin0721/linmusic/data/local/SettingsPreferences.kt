package com.lin0721.linmusic.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 使用 preferencesDataStore 进行设置项持久化
private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class SettingsPreferences(private val context: Context) {

    companion object {
        // Wi-Fi 播放音质 KEY，默认 "lossless"
        private val KEY_WIFI_QUALITY = stringPreferencesKey("wifi_quality")
        // 移动网络播放音质 KEY，默认 "standard"
        private val KEY_MOBILE_QUALITY = stringPreferencesKey("mobile_quality")
        // 新建歌单是否默认设为隐私模式
        private val KEY_DEFAULT_PLAYLIST_PRIVATE = booleanPreferencesKey("default_playlist_private")
        // 默认搜索源
        private val KEY_DEFAULT_SEARCH_SOURCE = stringPreferencesKey("default_search_source")
        // 缓存开关 KEY，默认 true
        private val KEY_STREAM_CACHE_ENABLED = booleanPreferencesKey("stream_cache_enabled")
        // 音频缓存上限大小 KEY，默认 512MB
        private val KEY_AUDIO_CACHE_MAX_SIZE = longPreferencesKey("audio_cache_max_size")
    }

    // Wi-Fi 音质设置 Flow
    val wifiQuality: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_WIFI_QUALITY] ?: "lossless"
    }

    suspend fun saveWifiQuality(quality: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_WIFI_QUALITY] = quality
        }
    }

    // 移动网络音质设置 Flow
    val mobileQuality: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_MOBILE_QUALITY] ?: "standard"
    }

    suspend fun saveMobileQuality(quality: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_MOBILE_QUALITY] = quality
        }
    }

    // 默认隐私歌单配置 Flow
    val defaultPlaylistPrivate: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_PLAYLIST_PRIVATE] ?: false
    }

    suspend fun saveDefaultPlaylistPrivate(private: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_PLAYLIST_PRIVATE] = private
        }
    }

    // 默认搜索源 Flow
    val defaultSearchSource: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SEARCH_SOURCE] ?: "netease"
    }

    suspend fun saveDefaultSearchSource(source: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SEARCH_SOURCE] = source
        }
    }

    // 缓存开关设置 Flow
    val streamCacheEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_STREAM_CACHE_ENABLED] ?: true
    }

    suspend fun saveStreamCacheEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_STREAM_CACHE_ENABLED] = enabled
        }
    }

    // 音频缓存最大容量设置 Flow
    val audioCacheMaxSize: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_AUDIO_CACHE_MAX_SIZE] ?: (512 * 1024 * 1024L) // 默认 512MB
    }

    suspend fun saveAudioCacheMaxSize(size: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_AUDIO_CACHE_MAX_SIZE] = size
        }
    }
}


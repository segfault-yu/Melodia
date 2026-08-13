package com.lin0721.linmusic.core.player.data

import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.AppError
import com.lin0721.linmusic.core.network.apiFlow
import com.lin0721.linmusic.core.network.mapToAppError
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.player.domain.LyricParser
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class PlaybackRepositoryImpl(
    private val apiService: PlaybackApi,
    private val settingsPreferences: SettingsPreferences,
    private val userPreferences: UserPreferences,
    private val contentFilter: ContentFilter,
    private val context: android.content.Context
) : PlaybackRepository {

    private fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    override fun getSongUrl(songId: Long): Flow<Result<String>> = apiFlow(
        request = {
            val quality = if (isWifiConnected()) {
                settingsPreferences.wifiQuality.first()
            } else {
                settingsPreferences.mobileQuality.first()
            }
            apiService.getSongUrl(body = SongUrlRequest(ids = "[$songId]", level = quality))
        },
        // 该歌曲可能需要开启 VIP 或版权受限：code=200 但 url 为空，也算失败
        isSuccess = { it.isSuccess && !it.data.firstOrNull()?.url.isNullOrBlank() },
        code = { it.code },
        transform = { it.data.first().url!! }
    )

    override fun getLyrics(songId: Long): Flow<Result<List<LyricLine>>> = apiFlow(
        request = {
            apiService.getLyrics(
                LyricRequest(id = songId, tv = -1, lv = -1, rv = -1, kv = -1, ytv = -1, yrv = -1)
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { response ->
            when {
                // 纯音乐返回带标识的单行；未收录返回空列表以隐藏卡片
                response.nolyric -> listOf(LyricLine(timeMs = 0, text = "纯音乐"))
                response.uncollected -> emptyList()
                else -> {
                    val yrcText = response.yrc?.lyric
                    val lrcText = response.lrc?.lyric
                    // 检测歌词文本中是否包含“纯音乐”或“Instrumental”标识
                    val isInstrumental = (!yrcText.isNullOrBlank() && (yrcText.contains("纯音乐") || yrcText.contains("Instrumental", ignoreCase = true))) ||
                            (!lrcText.isNullOrBlank() && (lrcText.contains("纯音乐") || lrcText.contains("Instrumental", ignoreCase = true)))
                    if (isInstrumental) {
                        listOf(LyricLine(timeMs = 0, text = "纯音乐"))
                    } else {
                        val lines = if (!yrcText.isNullOrBlank()) {
                            val parsedYrc = LyricParser.parseYrc(yrcText)
                            if (parsedYrc.isNotEmpty()) parsedYrc else LyricParser.parseLrc(lrcText ?: "")
                        } else {
                            LyricParser.parseLrc(lrcText ?: "")
                        }
                        if (lines.isEmpty()) {
                            emptyList()
                        } else {
                            // 解析翻译歌词列表（优先使用 ytlrc，其次使用 tlyric）
                            val translationLines = LyricParser.parseLrc(response.ytlrc?.lyric ?: response.tlyric?.lyric ?: "")
                            lines.map { line ->
                                // 寻找在 150ms 内与原词时间戳最接近的翻译行
                                val matchedTranslation = translationLines
                                    .filter { kotlin.math.abs(it.timeMs - line.timeMs) < 150 }
                                    .minByOrNull { kotlin.math.abs(it.timeMs - line.timeMs) }
                                    ?.text
                                line.copy(translation = matchedTranslation)
                            }
                        }
                    }
                }
            }
        }
    )

    override fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>> = apiFlow(
        request = { apiService.getSimiSongs(SimiSongRequest(songid = songId.toString())) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { contentFilter.filterBlockedArtists(it.songs) { song -> song.ar.map { a -> a.id } } }
    )

    // 含降级逻辑（心动模式失败自动回退相似歌曲），复杂度超出 apiFlow 模板范围，保留手写 flow
    override fun getIntelligenceSongs(songId: Long, playlistId: Long): Flow<Result<List<Track>>> = flow {
        var success = false
        var tracksList = emptyList<Track>()

        val finalPlaylistId = if (playlistId == 0L) {
            userPreferences.userProfile.first()?.uid ?: 0L
        } else {
            playlistId
        }

        if (finalPlaylistId != 0L) {
            try {
                val response = apiService.getIntelligenceSongs(
                    IntelligenceSongsRequest(
                        songId = songId.toString(),
                        playlistId = finalPlaylistId.toString(),
                        startMusicId = songId.toString(),
                        count = 20
                    )
                )
                if (response.isSuccess) {
                    val tracks = response.data.mapNotNull { it.songInfo }
                    if (tracks.isNotEmpty()) {
                        tracksList = tracks
                        success = true
                    }
                }
            } catch (_: Exception) {
                // 捕获智能推荐的异常
            }
        }

        if (success) {
            val filteredTracks = contentFilter.filterBlockedArtists(tracksList) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredTracks))
        } else {
            // 若心动模式报错或不支持，自动通过相似歌曲接口获取推荐
            // 注意：emit 必须放在 try/catch 之外，原因同上（避免误捕获 .first() 的内部取消信号）
            val fallbackResult = try {
                val simiResponse = apiService.getSimiSongs(SimiSongRequest(songid = songId.toString()))
                if (simiResponse.isSuccess) {
                    val filteredSimi = contentFilter.filterBlockedArtists(simiResponse.songs) { it.ar.map { a -> a.id } }
                    Result.success(filteredSimi)
                } else {
                    Result.failure(AppError.BizError(simiResponse.code, null))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            emit(fallbackResult)
        }
    }.catch { e ->
        emit(Result.failure(mapToAppError(e)))
    }
}

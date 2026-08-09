package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.local.SettingsPreferences
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.contentfilter.ContentFilter
import okhttp3.MediaType.Companion.toMediaType
import com.lin0721.linmusic.core.api.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val apiService: NeteaseApiService,
    private val settingsPreferences: SettingsPreferences,
    private val userPreferences: UserPreferences,
    private val contentFilter: ContentFilter,
    private val context: android.content.Context
) : MusicRepository {

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

    override fun getSongUrl(songId: Long): Flow<Result<String>> = flow {
        val quality = if (isWifiConnected()) {
            settingsPreferences.wifiQuality.first()
        } else {
            settingsPreferences.mobileQuality.first()
        }

        val response = apiService.getSongUrl(
            body = SongUrlRequest(
                ids = "[$songId]",
                level = quality
            )
        )

        if (response.isSuccess) {
            val songItem = response.data.firstOrNull()
            if (songItem != null && !songItem.url.isNullOrBlank()) {
                emit(Result.success(songItem.url))
            } else {
                emit(Result.failure(Exception("无法获取播放链接：该歌曲可能需要开启 VIP 或其版权受限。")))
            }
        } else {
            emit(Result.failure(Exception("网易云 API 响应异常 (Code: ${response.code})，可能是风控拦截。")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun createPlaylist(name: String, privacy: Int): Flow<Result<PlaylistDetail>> = flow {
        val response = apiService.createPlaylist(com.lin0721.linmusic.core.api.PlaylistCreateRequest(name = name, privacy = privacy))
        if (response.isSuccess && response.playlist != null) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to create playlist: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getLyrics(songId: Long): Flow<Result<List<LyricLine>>> = flow {
        val response = apiService.getLyrics(
            com.lin0721.linmusic.core.api.LyricRequest(
                id = songId,
                tv = -1,
                lv = -1,
                rv = -1,
                kv = -1,
                ytv = -1,
                yrv = -1
            )
        )
        if (!response.isSuccess) {
            emit(Result.failure(Exception("Failed to load lyrics: code ${response.code}")))
            return@flow
        }

        // 如果是纯音乐，返回带有“纯音乐”标识的单个歌词行；若是未收录，则返回空列表以隐藏卡片
        if (response.nolyric) {
            emit(Result.success(listOf(LyricLine(timeMs = 0, text = "纯音乐"))))
            return@flow
        }
        if (response.uncollected) {
            emit(Result.success(emptyList()))
            return@flow
        }

        val yrcText = response.yrc?.lyric
        val lrcText = response.lrc?.lyric

        // 检测歌词文本内容中是否包含“纯音乐”或“Instrumental”标识，若包含则视为纯音乐并返回对应标识以隐藏卡片
        val isInstrumental = (!yrcText.isNullOrBlank() && (yrcText.contains("纯音乐") || yrcText.contains("Instrumental", ignoreCase = true))) ||
                (!lrcText.isNullOrBlank() && (lrcText.contains("纯音乐") || lrcText.contains("Instrumental", ignoreCase = true)))
        if (isInstrumental) {
            emit(Result.success(listOf(LyricLine(timeMs = 0, text = "纯音乐"))))
            return@flow
        }

        val lines = if (!yrcText.isNullOrBlank()) {
            val parsedYrc = parseYrc(yrcText)
            if (parsedYrc.isNotEmpty()) parsedYrc else parseLrc(lrcText ?: "")
        } else {
            parseLrc(lrcText ?: "")
        }

        if (lines.isEmpty()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        // 解析翻译歌词列表（优先使用 ytlrc，其次使用 tlyric）
        val translationLines = parseLrc(response.ytlrc?.lyric ?: response.tlyric?.lyric ?: "")
        val finalLines = lines.map { line ->
            // 寻找在 150ms 内与原词时间戳最接近的翻译行
            val matchedTranslation = translationLines
                .filter { kotlin.math.abs(it.timeMs - line.timeMs) < 150 }
                .minByOrNull { kotlin.math.abs(it.timeMs - line.timeMs) }
                ?.text
                
            line.copy(translation = matchedTranslation)
        }
        emit(Result.success(finalLines))
    }.catch { e ->
        emit(Result.failure(e))
    }

    private val yrcLineRegex = Regex("""^\[(\d+),(\d+)](.*)$""")
    private val yrcWordRegex = Regex("""\((\d+),(\d+),\d+\)([^(\n]+)""")

    private fun parseYrc(yrcText: String): List<LyricLine> {
        return yrcText.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            
            yrcLineRegex.find(trimmed)?.let { match ->
                val lineStartTime = match.groupValues[1].toLongOrNull() ?: return@let null
                val lineDuration = match.groupValues[2].toLongOrNull() ?: return@let null
                val wordsContent = match.groupValues[3]
                
                val wordsList = mutableListOf<WordInfo>()
                val fullTextBuilder = StringBuilder()
                
                yrcWordRegex.findAll(wordsContent).forEach { wordMatch ->
                    val absoluteTime = wordMatch.groupValues[1].toLongOrNull() ?: 0L
                    val startOffset = absoluteTime - lineStartTime // 计算相对于行开始时间的偏移量
                    val duration = wordMatch.groupValues[2].toLongOrNull() ?: 0L
                    val wordText = wordMatch.groupValues[3]
                    
                    wordsList.add(WordInfo(wordText, startOffset, duration))
                    fullTextBuilder.append(wordText)
                }
                
                LyricLine(
                    timeMs = lineStartTime,
                    durationMs = lineDuration,
                    text = fullTextBuilder.toString(),
                    words = wordsList
                )
            }
        }.sortedBy { it.timeMs }
    }

    private val lrcPattern = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})](.*)""")

    private fun parseLrc(lrcText: String): List<LyricLine> {
        return lrcText.lines().mapNotNull { line ->
            lrcPattern.find(line)?.let { match ->
                val min = match.groupValues[1].toLongOrNull() ?: return@let null
                val sec = match.groupValues[2].toLongOrNull() ?: return@let null
                val msRaw = match.groupValues[3]
                val ms = if (msRaw.length == 2) msRaw.toLong() * 10 else msRaw.toLong()
                val text = match.groupValues[4].trim()
                if (text.isEmpty()) return@let null
                LyricLine(timeMs = min * 60_000 + sec * 1000 + ms, text = text)
            }
        }.sortedBy { it.timeMs }
    }

    private fun parseLrcToMap(lrcText: String?): Map<Long, String> {
        if (lrcText.isNullOrBlank()) return emptyMap()
        return parseLrc(lrcText).associate { it.timeMs to it.text }
    }

    override fun getSongDetail(songId: Long): Flow<Result<com.lin0721.linmusic.core.api.Track>> = flow {
        val c = """[{"id":$songId}]"""
        val response = apiService.getSongDetail(SongDetailRequest(c = c))
        if (response.isSuccess && response.songs.isNotEmpty()) {
            emit(Result.success(response.songs[0]))
        } else {
            emit(Result.failure(Exception("Failed to load song detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>> = flow {
        // 传入 songId.toString()
        val response = apiService.getSimiSongs(SimiSongRequest(songid = songId.toString()))
        if (response.isSuccess) {
            val filteredTracks = contentFilter.filterBlockedArtists(response.songs) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredTracks))
        } else {
            emit(Result.failure(Exception("获取相似歌曲失败: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

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
            try {
                val simiResponse = apiService.getSimiSongs(SimiSongRequest(songid = songId.toString()))
                if (simiResponse.isSuccess) {
                    val filteredSimi = contentFilter.filterBlockedArtists(simiResponse.songs) { it.ar.map { a -> a.id } }
                    emit(Result.success(filteredSimi))
                } else {
                    emit(Result.failure(Exception("获取智能推荐与相似推荐均失败")))
                }
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    // 获取合并后的歌曲详情与百科信息
    override fun getSongWiki(songId: Long): Flow<Result<SongWikiData>> = flow {
        coroutineScope {
            // 并发请求三个核心接口
            val detailDeferred = async {
                runCatching {
                    val c = """[{"id":$songId}]"""
                    apiService.getSongDetail(SongDetailRequest(c = c))
                }
            }
            val wikiDeferred = async {
                runCatching {
                    apiService.getSongWikiSummary(SongWikiSummaryRequest(songId = songId))
                }
            }
            val creatorsDeferred = async {
                runCatching {
                    apiService.getSongCreators(SongCreatorsRequest(songId = songId))
                }
            }

            val detailResult = detailDeferred.await().getOrNull()
            val wikiResult = wikiDeferred.await().getOrNull()
            val creatorsResult = creatorsDeferred.await().getOrNull()

            // 解析基础数据：专辑名与发行时间
            val albumName = detailResult?.songs?.firstOrNull()?.al?.name ?: ""
            val publishTime = detailResult?.songs?.firstOrNull()?.publishTime ?: 0L
            val publishDateStr = if (publishTime > 0) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(publishTime))
            } else {
                ""
            }

            var style = ""
            var language = ""
            var bpm = ""
            var entertainment = ""
            var background = ""
            var awards = ""

            // 解析百科简要信息中的 Block 列表
            wikiResult?.data?.blocks?.forEach { block ->
                if (block.code == "SONG_PLAY_ABOUT_SONG_BASIC") {
                    block.creatives.forEach { creative ->
                        when (creative.creativeType) {
                            "songTag" -> {
                                style = creative.resources.mapNotNull { it.uiElement?.mainTitle?.title }
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" / ")
                            }
                            "language" -> {
                                language = creative.uiElement?.textLinks?.firstOrNull()?.text ?: ""
                            }
                            "bpm" -> {
                                bpm = creative.uiElement?.textLinks?.firstOrNull()?.text ?: ""
                            }
                            "entertainment" -> {
                                entertainment = creative.resources.mapNotNull { it.uiElement?.mainTitle?.title }
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" / ")
                            }
                        }
                    }
                } else if (block.code == "SONG_PLAY_ABOUT_WIKI") {
                    // 解析歌曲百科模块，提取歌曲背景描述以及所获奖项/荣誉
                    block.creatives.forEach { creative ->
                        val creativeType = creative.creativeType
                        val creativeTitle = creative.uiElement?.mainTitle?.title ?: ""
                        
                        val descriptions = mutableListOf<String>()
                        creative.uiElement?.descriptions?.forEach { desc ->
                            if (desc.description.isNotEmpty()) {
                                descriptions.add(desc.description)
                            }
                        }
                        creative.resources.forEach { res ->
                            res.uiElement?.descriptions?.forEach { desc ->
                                if (desc.description.isNotEmpty()) {
                                    descriptions.add(desc.description)
                                }
                            }
                        }
                        val contentText = descriptions.joinToString("\n")
                        
                        if (creativeType == "background" || creativeTitle.contains("背景") || creativeTitle.contains("故事")) {
                            if (contentText.isNotEmpty()) {
                                background = contentText
                            }
                        } else if (creativeType == "awards" || creativeTitle.contains("奖项") || creativeTitle.contains("获奖") || creativeTitle.contains("荣誉") || creativeTitle.contains("排行")) {
                            if (contentText.isNotEmpty()) {
                                awards = contentText
                            }
                        }
                    }
                }
            }

            // 解析制作人员信息：提取全部角色，拼接为详细制作名单
            val creatorRoles = creatorsResult?.data?.songCreatorsRoleVos
            val creatorsStr = if (!creatorRoles.isNullOrEmpty()) {
                val parts = mutableListOf<String>()
                creatorRoles.forEach { role ->
                    val artists = role.creatorMetaVOS.map { it.artistName }.filter { it.isNotEmpty() }
                    if (artists.isNotEmpty()) {
                        parts.add("${role.roleName} ${artists.joinToString(" ")}")
                    }
                }
                parts.joinToString(" / ")
            } else {
                ""
            }

            emit(Result.success(
                SongWikiData(
                    style = style,
                    album = albumName,
                    language = language,
                    publishTime = publishDateStr,
                    bpm = bpm,
                    creators = creatorsStr,
                    entertainment = entertainment,
                    background = background,
                    awards = awards
                )
            ))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getUserLevel(): Flow<Result<UserLevelData>> = flow {
        val response = apiService.getUserLevel()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("获取用户等级失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getVipInfo(): Flow<Result<VipInfoData>> = flow {
        val response = apiService.getVipInfo()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("获取VIP信息失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getUserBindings(uid: Long): Flow<Result<List<UserBindingItem>>> = flow {
        val response = apiService.getUserBindings(UserBindingRequest(uid = uid))
        if (response.isSuccess) {
            emit(Result.success(response.bindings))
        } else {
            emit(Result.failure(Exception("获取账号绑定信息失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun updateUserProfile(
        nickname: String,
        gender: Int,
        birthday: Long,
        province: Int,
        city: Int,
        signature: String
    ): Flow<Result<Unit>> = flow {
        val response = apiService.updateUserProfile(
            UserProfileUpdateRequest(
                nickname = nickname,
                gender = gender,
                birthday = birthday,
                province = province,
                city = city,
                signature = signature
            )
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("修改个人资料失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun checkNickname(nickname: String): Flow<Result<Boolean>> = flow {
        val response = apiService.checkNickname(NicknameCheckRequest(nickname = nickname))
        if (response.isSuccess) {
            emit(Result.success(response.duplicated))
        } else {
            emit(Result.failure(Exception("检查昵称重名失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun dailySignin(type: Int): Flow<Result<Int>> = flow {
        val response = apiService.dailySignin(DailySigninRequest(type = type))
        if (response.isSuccess) {
            emit(Result.success(response.point))
        } else if (response.code == -2) {
            emit(Result.success(0))
        } else {
            emit(Result.failure(Exception(response.msg ?: "签到失败")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun uploadAvatar(file: java.io.File): Flow<Result<String>> = flow {
        val mediaType = "image/*".toMediaType()
        val requestFile = okhttp3.RequestBody.create(mediaType, file)
        val body = okhttp3.MultipartBody.Part.createFormData("imgFile", file.name, requestFile)
        val response = apiService.uploadAvatar(body)
        if (response.isSuccess) {
            emit(Result.success(response.url ?: ""))
        } else {
            emit(Result.failure(Exception("更换头像失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}

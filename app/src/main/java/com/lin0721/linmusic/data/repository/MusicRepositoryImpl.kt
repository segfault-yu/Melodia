package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.local.SettingsPreferences
import com.lin0721.linmusic.data.local.UserPreferences
import okhttp3.MediaType.Companion.toMediaType
import com.lin0721.linmusic.core.api.*
import com.lin0721.linmusic.ui.home.*
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
    private val context: android.content.Context
) : MusicRepository {

    override fun getDefaultSearchKeyword(): Flow<Result<String>> = flow {
        val response = apiService.getSearchDefaultKeyword()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data.showKeyword))
        } else {
            emit(Result.failure(Exception("Failed to get default keyword")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun searchSongs(keyword: String, offset: Int, limit: Int): Flow<Result<SearchSongsResult>> = flow {
        val response = apiService.cloudSearch(CloudSearchRequest(s = keyword, offset = offset, limit = limit))
        if (response.isSuccess && response.result != null) {
            val songs = response.result.songs ?: emptyList()
            val total = response.result.songCount
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredSongs = songs.filter { song ->
                song.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            val hasMore = if (filteredSongs.isEmpty()) false else (offset + songs.size < total)
            emit(Result.success(SearchSongsResult(filteredSongs, total, hasMore)))
        } else {
            emit(Result.failure(Exception("搜索失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getHotSearches(): Flow<Result<List<HotSearch>>> = flow {
        val response = apiService.getHotSearchDetail()
        if (response.isSuccess) {
            val list = response.data.map { item ->
                HotSearch(
                    keyword = item.searchWord,
                    score = item.score,
                    description = item.content,
                    iconUrl = item.iconUrl
                )
            }
            emit(Result.success(list))
        } else {
            emit(Result.failure(Exception("获取热搜失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getPlaylistTags(): Flow<Result<List<PlaylistTag>>> = flow {
        val (tags, playlists) = coroutineScope {
            val tagsDeferred = async { apiService.getHighQualityTags() }
            val playlistsDeferred = async {
                apiService.getHighQualityPlaylists(HighQualityPlaylistRequest(limit = 50))
            }
            tagsDeferred.await() to playlistsDeferred.await()
        }

        if (!tags.isSuccess) {
            emit(Result.failure(Exception("获取标签失败: code ${tags.code}")))
            return@flow
        }

        val coverMap = mutableMapOf<String, String>()
        if (playlists.isSuccess) {
            for (playlist in playlists.playlists) {
                for (tag in playlist.tags) {
                    if (tag !in coverMap && playlist.coverImgUrl.isNotBlank()) {
                        coverMap[tag] = "${playlist.coverImgUrl}?param=400y400"
                    }
                }
            }
        }

        val result = tags.tags
            .filter { it.name != "全部" }
            .map { tag ->
                PlaylistTag(
                    name = tag.name,
                    coverUrl = coverMap[tag.name] ?: ""
                )
            }

        emit(Result.success(result))
    }.catch { e -> emit(Result.failure(e)) }

    override fun getDiscoveryBlocks(refresh: Boolean, cursor: String?): Flow<Result<List<HomepageBlock>>> = flow {
        val response = apiService.getHomepageBlocks(HomepageBlockRequest(cursor = cursor, refresh = refresh))
        if (response.code == 200 && response.data != null) {
            emit(Result.success(response.data.blocks))
        } else {
            emit(Result.failure(Exception("Failed to load discovery blocks")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getHomepageBlocks(refresh: Boolean, cursor: String?): Flow<Result<HomeFeedPage>> = flow {
        val response = apiService.getHomepageBlocks(HomepageBlockRequest(cursor = cursor, refresh = refresh))
        if (response.code == 200 && response.data != null) {
            val data = response.data
            val sections = data.blocks.mapNotNull { block ->
                val title = block.uiElement?.mainTitle?.title ?: ""
                
                val items = if (block.blockCode == "HOMEPAGE_BLOCK_STYLE_RCMD" || block.blockCode == "HOMEPAGE_BLOCK_MGC_PLAYLIST") {
                    block.creatives?.mapIndexed { index, creative ->
                        CardItem(
                            id = if (creative.targetId != 0L) creative.targetId.toString() else (creative.creativeId.takeIf { it.isNotBlank() } ?: "creative_$index"),
                            title = creative.uiElement?.mainTitle?.title ?: "",
                            subtitle = creative.uiElement?.subTitle?.title,
                            imageUrl = creative.uiElement?.image?.imageUrl ?: "",
                            type = CardType.PLAYLIST
                        )
                    } ?: emptyList()
                } else {
                    block.creatives?.flatMapIndexed { cIdx, creative ->
                        creative.resources?.mapIndexed { rIdx, resource ->
                            CardItem(
                                id = if (resource.targetId != 0L) resource.targetId.toString() else (resource.resourceId.takeIf { it.isNotBlank() } ?: "res_${cIdx}_$rIdx"),
                                title = resource.uiElement?.mainTitle?.title ?: "",
                                subtitle = resource.uiElement?.subTitle?.title,
                                imageUrl = creative.uiElement?.image?.imageUrl ?: "",
                                isSong = resource.resourceType == "song",
                                type = when (resource.resourceType) {
                                    "playlist" -> CardType.PLAYLIST
                                    "album" -> CardType.ALBUM
                                    "artist" -> CardType.ARTIST
                                    else -> CardType.PLAYLIST
                                }
                            )
                        } ?: listOf(
                            CardItem(
                                id = if (creative.targetId != 0L) creative.targetId.toString() else (creative.creativeId.takeIf { it.isNotBlank() } ?: "cre_${cIdx}"),
                                title = creative.uiElement?.mainTitle?.title ?: "",
                                subtitle = creative.uiElement?.subTitle?.title,
                                imageUrl = creative.uiElement?.image?.imageUrl ?: "",
                                type = CardType.PLAYLIST
                            )
                        )
                    } ?: emptyList()
                }

                if (items.isNotEmpty()) {
                    when {
                        block.blockCode == "HOMEPAGE_BLOCK_STYLE_RCMD" || block.blockCode == "HOMEPAGE_BLOCK_MGC_PLAYLIST" -> HomeSection.SectionMixes(title, items)
                        block.showType == "SLIDE_PLAYABLE_DRAGON_BALL" || block.blockCode == "HOMEPAGE_BLOCK_STYLE_ARTIST" -> HomeSection.SectionArtist(title, items)
                        else -> HomeSection.SectionCarousel(title, items)
                    }
                } else null
            }
            emit(Result.success(HomeFeedPage(sections, data.cursor, data.hasMore)))
        } else {
            emit(Result.failure(Exception("Failed to load homepage blocks: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>> = flow {
        val response = apiService.getPersonalizedPlaylists()
        if (response.isSuccess) {
            emit(Result.success(PersonalizedData(playlists = response.result)))
        } else {
            emit(Result.failure(Exception("Failed to load personalized playlists: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>> = flow {
        val response = apiService.getPlaylistDetail(PlaylistDetailRequest(id = id))
        if (response.isSuccess && response.playlist != null) {
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredTracks = response.playlist.tracks.filter { track ->
                track.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            emit(Result.success(response.playlist.copy(tracks = filteredTracks)))
        } else {
            emit(Result.failure(Exception("Failed to load playlist detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>> = flow {
        // 专辑 ID 需作为 URL 路径参数传入，不使用 AlbumDetailRequest 请求体
        val response = apiService.getAlbumDetail(id = id)
        if (response.isSuccess) {
            val album = response.album
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredTracks = response.songs.filter { track ->
                track.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            val detail = PlaylistDetail(
                id = album.id,
                name = album.name,
                coverImgUrl = album.picUrl,
                description = album.description,
                playCount = 0L,
                tracks = filteredTracks
            )
            emit(Result.success(detail))
        } else {
            emit(Result.failure(Exception("Failed to load album detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

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

    override fun getTopArtists(): Flow<Result<List<Artist>>> = flow {
        val response = apiService.getTopArtists()
        if (response.isSuccess) {
            emit(Result.success(response.artists))
        } else {
            emit(Result.failure(Exception("API Error (Code: ${response.code})")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getFavoriteArtists(): Flow<Result<List<ArtistInfo>>> = flow {
        var artists = emptyList<ArtistInfo>()

        // 尝试获取已关注歌手（实际返回: {"data":[...], "code":200}）
        try {
            val response = apiService.getArtistSublist()
            if (response.code == 200 && response.data.isNotEmpty()) {
                artists = response.data.map { dto ->
                    ArtistInfo(
                        id = dto.id,
                        name = dto.name,
                        avatarUrl = dto.img1v1Url.takeIf { it.isNotBlank() } ?: dto.picUrl
                    )
                }
            }
        } catch (_: Exception) {
            // 网络失败或服务器返回空体，进入备用流程
        }


        if (artists.isNotEmpty()) {
            emit(Result.success(artists))
            return@flow
        }

        // \u5907\u7528\uff1a\u70ed\u95e8\u6b4c\u624b\u699c\u5355
        try {
            val response = apiService.getTopArtists()
            if (response.isSuccess && response.artists.isNotEmpty()) {
                artists = response.artists.map { dto ->
                    ArtistInfo(
                        id = dto.id,
                        name = dto.name,
                        avatarUrl = dto.img1v1Url.takeIf { it.isNotBlank() } ?: dto.picUrl
                    )
                }
                emit(Result.success(artists))
            } else {
                emit(Result.failure(Exception("API Error (Code: ${response.code})")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getAccountInfo(): Flow<Result<com.lin0721.linmusic.core.api.AccountInfoResponse>> = flow {
        val response = apiService.getAccountInfo()
        if (response.code == 200) {
            emit(Result.success(response))
        } else {
            emit(Result.failure(Exception("Failed to get account info: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getRecentPlaylists(): Flow<Result<List<RecentPlayItem>>> = flow {
        val response = apiService.getRecentPlaylists()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data.list))
        } else {
            emit(Result.failure(Exception("Failed to load recent playlists: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getDailyRecommendSongs(): Flow<Result<List<DailySong>>> = flow {
        val response = apiService.getDailyRecommendSongs()
        if (response.isSuccess && response.data != null) {
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredSongs = response.data.dailySongs.filter { song ->
                song.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            emit(Result.success(filteredSongs))
        } else {
            emit(Result.failure(Exception("Failed to load daily recommend songs: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getHistoryRecommendDates(): Flow<Result<List<String>>> = flow {
        val response = apiService.getHistoryRecommendDates()
        if (response.code == 200 && response.data != null) {
            emit(Result.success(response.data.list))
        } else {
            emit(Result.failure(Exception("Failed to load history dates: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getHistoryRecommendDetail(date: String): Flow<Result<List<DailySong>>> = flow {
        val response = apiService.getHistoryRecommendDetail(HistoryDetailRequest(date = date))
        if (response.code == 200 && response.data != null) {
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredSongs = response.data.dailySongs.filter { song ->
                song.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            emit(Result.success(filteredSongs))
        } else {
            emit(Result.failure(Exception("Failed to load history detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getToplistDetail(): Flow<Result<List<ToplistInfo>>> = flow {
        val response = apiService.getToplistDetail()
        if (response.code == 200) {
            val domainList = response.list
                // 过滤封面图为空的无效榜单条目
                .filter { it.coverImgUrl.isNotBlank() && it.name.isNotBlank() }
                .map { dto ->
                    ToplistInfo(
                        id = dto.id,
                        name = dto.name,
                        coverUrl = "${dto.coverImgUrl}?param=300y300",
                        updateDesc = dto.updateFrequency,
                        topSongs = dto.tracks?.map { "${it.first} - ${it.second}" } ?: emptyList()
                    )
                }
            emit(Result.success(domainList))
        } else {
            emit(Result.failure(Exception("Failed to load toplist: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getUserPlaylists(uid: Long, limit: Int): Flow<Result<List<com.lin0721.linmusic.core.api.UserPlaylist>>> = flow {
        val response = apiService.getUserPlaylists(com.lin0721.linmusic.core.api.UserPlaylistRequest(uid = uid, limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to load user playlists: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getUserRecord(uid: Long, type: Int): Flow<Result<List<Track>>> = flow {
        val response = apiService.getUserRecord(UserRecordRequest(uid = uid, type = type))
        if (response.isSuccess) {
            val list = if (type == 1) {
                response.weekData?.map { it.song } ?: emptyList()
            } else {
                response.allData?.map { it.song } ?: emptyList()
            }
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredTracks = list.filter { track ->
                track.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            emit(Result.success(filteredTracks))
        } else {
            emit(Result.failure(Exception("获取听歌排行失败: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getCollectedAlbums(limit: Int): Flow<Result<List<com.lin0721.linmusic.core.api.AlbumSubItem>>> = flow {
        val response = apiService.getAlbumSublist(com.lin0721.linmusic.core.api.AlbumSublistRequest(limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("Failed to load collected albums: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getUserSubcount(): Flow<Result<com.lin0721.linmusic.core.api.UserSubcountResponse>> = flow {
        val response = apiService.getUserSubcount()
        if (response.isSuccess) {
            emit(Result.success(response))
        } else {
            emit(Result.failure(Exception("Failed to load user subcount: code ${response.code}")))
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

    override fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>> = flow {
        val response = apiService.getSimiArtists(SimiArtistRequest(artistid = artistId))
        if (response.isSuccess) {
            val artists = response.artists.map { artist ->
                ArtistInfo(
                    id = artist.id,
                    name = artist.name,
                    avatarUrl = artist.img1v1Url.ifEmpty { artist.picUrl }
                )
            }
            emit(Result.success(artists))
        } else {
            emit(Result.failure(Exception("Failed to load similar artists: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>> = flow {
        // 传入 songId.toString()
        val response = apiService.getSimiSongs(SimiSongRequest(songid = songId.toString()))
        if (response.isSuccess) {
            val blockedIds = userPreferences.blockedArtistIds.first()
            val filteredTracks = response.songs.filter { track ->
                track.ar.none { artist -> blockedIds.contains(artist.id) }
            }
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

        val blockedIds = userPreferences.blockedArtistIds.first()
        if (success) {
            val filteredTracks = tracksList.filter { track ->
                track.ar.none { artist -> blockedIds.contains(artist.id) }
            }
            emit(Result.success(filteredTracks))
        } else {
            // 若心动模式报错或不支持，自动通过相似歌曲接口获取推荐
            try {
                val simiResponse = apiService.getSimiSongs(SimiSongRequest(songid = songId.toString()))
                if (simiResponse.isSuccess) {
                    val filteredSimi = simiResponse.songs.filter { track ->
                        track.ar.none { artist -> blockedIds.contains(artist.id) }
                    }
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

    override fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>> = flow {
        val response = apiService.getArtistDetail(ArtistDetailRequest(id = artistId))
        if (response.isSuccess && response.data?.artist != null) {
            emit(Result.success(response.data.artist))
        } else {
            emit(Result.failure(Exception("Failed to load artist detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getArtistAlbums(artistId: Long, limit: Int): Flow<Result<List<ArtistAlbum>>> = flow {
        val response = apiService.getArtistAlbums(id = artistId, body = ArtistAlbumRequest(limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.hotAlbums))
        } else {
            emit(Result.failure(Exception("Failed to load artist albums: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    // 获取歌手粉丝数量
    override fun getArtistFansCount(artistId: Long): Flow<Result<Long>> = flow {
        
        val response = apiService.getArtistFollowCount(ArtistFollowCountRequest(id = artistId))
        if (response.isSuccess) {
            val data = response.data
            val fans = data?.fansCnt ?: data?.fansCount ?: data?.fans ?: 0L
            emit(Result.success(fans))
        } else {
            emit(Result.failure(Exception("Failed to load artist fans count: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>> = flow {
        val response = apiService.getArtistTopSongs(ArtistTopSongsRequest(id = artistId))
        if (response.isSuccess) {
            emit(Result.success(response.songs))
        } else {
            emit(Result.failure(Exception("Failed to load artist top songs: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun subscribeArtist(artistId: Long, subscribe: Boolean): Flow<Result<Unit>> = flow {
        val op = if (subscribe) "sub" else "unsub"
        val response = apiService.subscribeArtist(
            op = op,
            body = ArtistSubscriptionRequest(
                artistId = artistId,
                artistIds = "[$artistId]"
            )
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("操作失败: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>> = flow {
        val response = apiService.getArtistDetailDynamic(ArtistFollowCountRequest(id = artistId))
        if (response.isSuccess) {
            emit(Result.success(response.isFollow))
        } else {
            emit(Result.failure(Exception("Failed to check artist follow state: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>> = flow {
        val response = apiService.getLikedSongIds(LikeSongListRequest(uid = uid))
        if (response.isSuccess) {
            emit(Result.success(response.ids))
        } else {
            emit(Result.failure(Exception("Failed to load liked songs: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>> = flow {
        val response = apiService.likeSong(LikeSongRequest(trackId = songId, like = like))
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Failed to like song: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun manipulatePlaylistTracks(op: String, playlistId: Long, trackId: Long): Flow<Result<Unit>> = flow {
        val response = apiService.manipulatePlaylistTracks(
            PlaylistTracksManipulateRequest(
                op = op,
                pid = playlistId,
                trackIds = "[\"$trackId\"]"
            )
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Failed to manipulate tracks: code ${response.code}, message ${response.message}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>> = flow {
        val op = if (subscribe) "subscribe" else "unsubscribe"
        val response = apiService.subscribePlaylist(
            op = op,
            body = PlaylistSubscribeRequest(id = playlistId)
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("操作失败: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }


    override fun getComments(songId: Long, limit: Int, offset: Int): Flow<Result<CommentsResponse>> = 
        getComments(threadId = "R_SO_4_$songId", limit = limit, offset = offset)

    override fun getComments(threadId: String, limit: Int, offset: Int): Flow<Result<CommentsResponse>> = flow {
        val response = apiService.getComments(
            threadId = threadId,
            body = CommentsRequest(
                threadId = threadId,
                rid = threadId.substringAfterLast("_"),
                limit = limit,
                offset = offset
            )
        )
        if (response.isSuccess) {
            emit(Result.success(response))
        } else {
            emit(Result.failure(Exception("Failed to load comments: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun likeComment(threadId: String, commentId: Long, like: Boolean): Flow<Result<Unit>> = flow {
        val op = if (like) "like" else "unlike"
        val response = apiService.likeComment(
            op = op,
            body = LikeCommentRequest(
                threadId = threadId,
                commentId = commentId
            )
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("操作失败: code ${response.code}, message: ${response.message}")))
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

    override fun logout(): Flow<Result<Unit>> = flow {
        val response = apiService.logoutApi()
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("退出登录接口异常")))
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

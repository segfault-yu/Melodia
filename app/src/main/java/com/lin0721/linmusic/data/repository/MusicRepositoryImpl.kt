package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.*
import com.lin0721.linmusic.ui.home.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val apiService: NeteaseApiService
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
            emit(Result.success(SearchSongsResult(songs, total, offset + songs.size < total)))
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
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to load playlist detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getSongUrl(songId: Long): Flow<Result<String>> = flow {
        val response = apiService.getSongUrl(SongUrlRequest(ids = "[$songId]"))

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

    override fun getAccountInfo(): Flow<Result<com.lin0721.linmusic.data.remote.api.AccountInfoResponse>> = flow {
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
            emit(Result.success(response.data.dailySongs))
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
            emit(Result.success(response.data.dailySongs))
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

    override fun getUserPlaylists(uid: Long, limit: Int): Flow<Result<List<com.lin0721.linmusic.data.remote.api.UserPlaylist>>> = flow {
        val response = apiService.getUserPlaylists(com.lin0721.linmusic.data.remote.api.UserPlaylistRequest(uid = uid, limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to load user playlists: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getCollectedAlbums(limit: Int): Flow<Result<List<com.lin0721.linmusic.data.remote.api.AlbumSubItem>>> = flow {
        val response = apiService.getAlbumSublist(com.lin0721.linmusic.data.remote.api.AlbumSublistRequest(limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("Failed to load collected albums: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getUserSubcount(): Flow<Result<com.lin0721.linmusic.data.remote.api.UserSubcountResponse>> = flow {
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
        val response = apiService.createPlaylist(com.lin0721.linmusic.data.remote.api.PlaylistCreateRequest(name = name, privacy = privacy))
        if (response.isSuccess && response.playlist != null) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to create playlist: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }
}

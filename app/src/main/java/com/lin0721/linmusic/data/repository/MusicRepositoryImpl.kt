package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.*
import com.lin0721.linmusic.ui.home.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val apiService: NeteaseApiService
) : MusicRepository {

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
}

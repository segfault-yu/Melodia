package com.lin0721.linmusic.feature.home.data

import com.lin0721.linmusic.core.api.DailySong
import com.lin0721.linmusic.core.api.HistoryDetailRequest
import com.lin0721.linmusic.core.api.HomepageBlockRequest
import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.api.PersonalizedData
import com.lin0721.linmusic.core.api.RecentPlayItem
import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.feature.home.domain.CardItem
import com.lin0721.linmusic.feature.home.domain.CardType
import com.lin0721.linmusic.feature.home.domain.HomeFeedPage
import com.lin0721.linmusic.feature.home.domain.HomeSection
import com.lin0721.linmusic.feature.home.domain.ToplistInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class HomeRepositoryImpl(
    private val apiService: NeteaseApiService,
    private val contentFilter: ContentFilter
) : HomeRepository {

    override fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>> = flow {
        val response = apiService.getPersonalizedPlaylists()
        if (response.isSuccess) {
            emit(Result.success(PersonalizedData(playlists = response.result)))
        } else {
            emit(Result.failure(Exception("Failed to load personalized playlists: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getRecentPlaylists(): Flow<Result<List<RecentPlayItem>>> = flow {
        val response = apiService.getRecentPlaylists()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data.list))
        } else {
            emit(Result.failure(Exception("Failed to load recent playlists: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

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
    }.catch { e -> emit(Result.failure(e)) }

    override fun getDailyRecommendSongs(): Flow<Result<List<DailySong>>> = flow {
        val response = apiService.getDailyRecommendSongs()
        if (response.isSuccess && response.data != null) {
            val filteredSongs = contentFilter.filterBlockedArtists(response.data.dailySongs) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredSongs))
        } else {
            emit(Result.failure(Exception("Failed to load daily recommend songs: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getHistoryRecommendDates(): Flow<Result<List<String>>> = flow {
        val response = apiService.getHistoryRecommendDates()
        if (response.code == 200 && response.data != null) {
            emit(Result.success(response.data.list))
        } else {
            emit(Result.failure(Exception("Failed to load history dates: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getHistoryRecommendDetail(date: String): Flow<Result<List<DailySong>>> = flow {
        val response = apiService.getHistoryRecommendDetail(HistoryDetailRequest(date = date))
        if (response.code == 200 && response.data != null) {
            val filteredSongs = contentFilter.filterBlockedArtists(response.data.dailySongs) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredSongs))
        } else {
            emit(Result.failure(Exception("Failed to load history detail: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    // 当前无调用者，疑似未完工的首页 Feed 流功能，保留待后续确认取舍
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
    }.catch { e -> emit(Result.failure(e)) }
}

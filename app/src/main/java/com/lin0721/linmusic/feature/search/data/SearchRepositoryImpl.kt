package com.lin0721.linmusic.feature.search.data

import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.AppError
import com.lin0721.linmusic.core.network.apiFlow
import com.lin0721.linmusic.core.network.mapToAppError
import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.feature.search.domain.SearchSongsResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

private const val TAG = "SearchRepositoryImpl"

class SearchRepositoryImpl(
    private val apiService: SearchApi,
    private val contentFilter: ContentFilter
) : SearchRepository {

    override fun getDefaultSearchKeyword(): Flow<Result<String>> = apiFlow(
        request = { apiService.getSearchDefaultKeyword() },
        isSuccess = { it.isSuccess && it.data != null },
        code = { it.code },
        transform = { it.data!!.showKeyword }
    )

    override fun searchSongs(keyword: String, offset: Int, limit: Int): Flow<Result<SearchSongsResult>> = apiFlow(
        request = { apiService.cloudSearch(CloudSearchRequest(s = keyword, offset = offset, limit = limit)) },
        isSuccess = { it.isSuccess && it.result != null },
        code = { it.code },
        transform = { response ->
            val songs = response.result!!.songs ?: emptyList()
            val total = response.result.songCount
            val filteredSongs = contentFilter.filterBlockedArtists(songs) { it.ar.map { a -> a.id } }
            val hasMore = if (filteredSongs.isEmpty()) false else (offset + songs.size < total)
            SearchSongsResult(filteredSongs, total, hasMore)
        }
    )

    override fun getHotSearches(): Flow<Result<List<HotSearch>>> = apiFlow(
        request = { apiService.getHotSearchDetail() },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { response ->
            response.data.map { item ->
                HotSearch(
                    keyword = item.searchWord,
                    score = item.score,
                    description = item.content,
                    iconUrl = item.iconUrl
                )
            }
        }
    )

    override fun getPlaylistTags(): Flow<Result<List<PlaylistTag>>> = flow {
        val (tags, playlists) = coroutineScope {
            val tagsDeferred = async { apiService.getHighQualityTags() }
            val playlistsDeferred = async {
                apiService.getHighQualityPlaylists(HighQualityPlaylistRequest(limit = 50))
            }
            tagsDeferred.await() to playlistsDeferred.await()
        }

        if (!tags.isSuccess) {
            AppLogger.e(TAG, "getPlaylistTags 标签接口业务失败 code=${tags.code}")
            emit(Result.failure(AppError.BizError(tags.code, null)))
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
        } else {
            AppLogger.w(TAG, "getPlaylistTags 精品歌单接口失败 code=${playlists.code}，封面图将缺失")
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
    }.catch { e ->
        AppLogger.e(TAG, "getPlaylistTags 请求异常", e)
        emit(Result.failure(mapToAppError(e)))
    }
}

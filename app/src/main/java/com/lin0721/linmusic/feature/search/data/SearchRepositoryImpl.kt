package com.lin0721.linmusic.feature.search.data

import com.lin0721.linmusic.core.api.CloudSearchRequest
import com.lin0721.linmusic.core.api.HighQualityPlaylistRequest
import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.feature.search.domain.SearchSongsResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class SearchRepositoryImpl(
    private val apiService: NeteaseApiService,
    private val contentFilter: ContentFilter
) : SearchRepository {

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
            val filteredSongs = contentFilter.filterBlockedArtists(songs) { it.ar.map { a -> a.id } }
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
}

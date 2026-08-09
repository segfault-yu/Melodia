package com.lin0721.linmusic.feature.library.data

import com.lin0721.linmusic.core.api.AlbumSubItem
import com.lin0721.linmusic.core.api.AlbumSublistRequest
import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.api.Track
import com.lin0721.linmusic.core.api.UserPlaylist
import com.lin0721.linmusic.core.api.UserPlaylistRequest
import com.lin0721.linmusic.core.api.UserRecordRequest
import com.lin0721.linmusic.core.api.UserSubcountResponse
import com.lin0721.linmusic.core.contentfilter.ContentFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class LibraryRepositoryImpl(
    private val apiService: NeteaseApiService,
    private val contentFilter: ContentFilter
) : LibraryRepository {

    override fun getUserPlaylists(uid: Long, limit: Int): Flow<Result<List<UserPlaylist>>> = flow {
        val response = apiService.getUserPlaylists(UserPlaylistRequest(uid = uid, limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to load user playlists: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getUserRecord(uid: Long, type: Int): Flow<Result<List<Track>>> = flow {
        val response = apiService.getUserRecord(UserRecordRequest(uid = uid, type = type))
        if (response.isSuccess) {
            val list = if (type == 1) {
                response.weekData?.map { it.song } ?: emptyList()
            } else {
                response.allData?.map { it.song } ?: emptyList()
            }
            val filteredTracks = contentFilter.filterBlockedArtists(list) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredTracks))
        } else {
            emit(Result.failure(Exception("获取听歌排行失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getCollectedAlbums(limit: Int): Flow<Result<List<AlbumSubItem>>> = flow {
        val response = apiService.getAlbumSublist(AlbumSublistRequest(limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("Failed to load collected albums: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getUserSubcount(): Flow<Result<UserSubcountResponse>> = flow {
        val response = apiService.getUserSubcount()
        if (response.isSuccess) {
            emit(Result.success(response))
        } else {
            emit(Result.failure(Exception("Failed to load user subcount: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}

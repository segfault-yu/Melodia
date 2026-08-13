package com.lin0721.linmusic.feature.library.data

import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow

class LibraryRepositoryImpl(
    private val apiService: LibraryApi,
    private val contentFilter: ContentFilter
) : LibraryRepository {

    override fun getUserPlaylists(uid: Long, limit: Int): Flow<Result<List<UserPlaylist>>> = apiFlow(
        request = { apiService.getUserPlaylists(UserPlaylistRequest(uid = uid, limit = limit)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.playlist }
    )

    override fun getUserRecord(uid: Long, type: Int): Flow<Result<List<Track>>> = apiFlow(
        request = { apiService.getUserRecord(UserRecordRequest(uid = uid, type = type)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { response ->
            val list = if (type == 1) {
                response.weekData?.map { it.song } ?: emptyList()
            } else {
                response.allData?.map { it.song } ?: emptyList()
            }
            contentFilter.filterBlockedArtists(list) { it.ar.map { a -> a.id } }
        }
    )

    override fun getCollectedAlbums(limit: Int): Flow<Result<List<AlbumSubItem>>> = apiFlow(
        request = { apiService.getAlbumSublist(AlbumSublistRequest(limit = limit)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.data }
    )

    override fun getUserSubcount(): Flow<Result<UserSubcountResponse>> = apiFlow(
        request = { apiService.getUserSubcount() },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it }
    )
}

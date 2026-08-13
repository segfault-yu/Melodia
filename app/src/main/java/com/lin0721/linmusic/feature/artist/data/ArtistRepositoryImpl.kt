package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ArtistRepositoryImpl(
    private val apiService: ArtistApi
) : ArtistRepository {

    override fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>> = apiFlow(
        request = { apiService.getArtistDetail(ArtistDetailRequest(id = artistId)) },
        isSuccess = { it.isSuccess && it.data?.artist != null },
        code = { it.code },
        transform = { it.data!!.artist!! }
    )

    override fun getArtistAlbums(artistId: Long, limit: Int): Flow<Result<List<ArtistAlbum>>> = apiFlow(
        request = { apiService.getArtistAlbums(id = artistId, body = ArtistAlbumRequest(limit = limit)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.hotAlbums }
    )

    // 获取歌手粉丝数量
    override fun getArtistFansCount(artistId: Long): Flow<Result<Long>> = apiFlow(
        request = { apiService.getArtistFollowCount(ArtistFollowCountRequest(id = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { val data = it.data; data?.fansCnt ?: data?.fansCount ?: data?.fans ?: 0L }
    )

    override fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>> = apiFlow(
        request = { apiService.getArtistTopSongs(ArtistTopSongsRequest(id = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.songs }
    )

    override fun subscribeArtist(artistId: Long, subscribe: Boolean): Flow<Result<Unit>> = apiFlow(
        request = {
            apiService.subscribeArtist(
                op = if (subscribe) "sub" else "unsub",
                body = ArtistSubscriptionRequest(
                    artistId = artistId,
                    artistIds = "[$artistId]"
                )
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { Unit }
    )

    override fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>> = apiFlow(
        request = { apiService.getArtistDetailDynamic(ArtistFollowCountRequest(id = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.isFollow }
    )

    override fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>> = apiFlow(
        request = { apiService.getSimiArtists(SimiArtistRequest(artistid = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { response ->
            response.artists.map { artist ->
                ArtistInfo(
                    id = artist.id,
                    name = artist.name,
                    avatarUrl = artist.img1v1Url.ifEmpty { artist.picUrl }
                )
            }
        }
    )
}

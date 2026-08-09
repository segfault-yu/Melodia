package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.api.Artist
import com.lin0721.linmusic.core.api.ArtistAlbum
import com.lin0721.linmusic.core.api.ArtistAlbumRequest
import com.lin0721.linmusic.core.api.ArtistDetailInfo
import com.lin0721.linmusic.core.api.ArtistDetailRequest
import com.lin0721.linmusic.core.api.ArtistFollowCountRequest
import com.lin0721.linmusic.core.api.ArtistSubscriptionRequest
import com.lin0721.linmusic.core.api.ArtistTopSongsRequest
import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.api.Track
import com.lin0721.linmusic.feature.artist.domain.ArtistInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class ArtistRepositoryImpl(
    private val apiService: NeteaseApiService
) : ArtistRepository {

    override fun getTopArtists(): Flow<Result<List<Artist>>> = flow {
        val response = apiService.getTopArtists()
        if (response.isSuccess) {
            emit(Result.success(response.artists))
        } else {
            emit(Result.failure(Exception("API Error (Code: ${response.code})")))
        }
    }.catch { e -> emit(Result.failure(e)) }

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

        // 备用：热门歌手榜单
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

    override fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>> = flow {
        val response = apiService.getArtistDetail(ArtistDetailRequest(id = artistId))
        if (response.isSuccess && response.data?.artist != null) {
            emit(Result.success(response.data.artist))
        } else {
            emit(Result.failure(Exception("Failed to load artist detail: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getArtistAlbums(artistId: Long, limit: Int): Flow<Result<List<ArtistAlbum>>> = flow {
        val response = apiService.getArtistAlbums(id = artistId, body = ArtistAlbumRequest(limit = limit))
        if (response.isSuccess) {
            emit(Result.success(response.hotAlbums))
        } else {
            emit(Result.failure(Exception("Failed to load artist albums: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

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
    }.catch { e -> emit(Result.failure(e)) }

    override fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>> = flow {
        val response = apiService.getArtistTopSongs(ArtistTopSongsRequest(id = artistId))
        if (response.isSuccess) {
            emit(Result.success(response.songs))
        } else {
            emit(Result.failure(Exception("Failed to load artist top songs: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

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
    }.catch { e -> emit(Result.failure(e)) }

    override fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>> = flow {
        val response = apiService.getArtistDetailDynamic(ArtistFollowCountRequest(id = artistId))
        if (response.isSuccess) {
            emit(Result.success(response.isFollow))
        } else {
            emit(Result.failure(Exception("Failed to check artist follow state: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>> = flow {
        val response = apiService.getSimiArtists(com.lin0721.linmusic.core.api.SimiArtistRequest(artistid = artistId))
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
    }.catch { e -> emit(Result.failure(e)) }
}

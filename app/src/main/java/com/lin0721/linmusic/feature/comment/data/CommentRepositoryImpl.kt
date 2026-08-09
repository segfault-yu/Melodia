package com.lin0721.linmusic.feature.comment.data

import com.lin0721.linmusic.core.api.CommentsRequest
import com.lin0721.linmusic.core.api.CommentsResponse
import com.lin0721.linmusic.core.api.LikeCommentRequest
import com.lin0721.linmusic.core.api.NeteaseApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class CommentRepositoryImpl(
    private val apiService: NeteaseApiService
) : CommentRepository {

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
    }.catch { e -> emit(Result.failure(e)) }

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
    }.catch { e -> emit(Result.failure(e)) }
}

package com.lin0721.linmusic.feature.comment.data

import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow

class CommentRepositoryImpl(
    private val apiService: CommentApi
) : CommentRepository {

    override fun getComments(songId: Long, limit: Int, offset: Int): Flow<Result<CommentsResponse>> =
        getComments(threadId = "R_SO_4_$songId", limit = limit, offset = offset)

    override fun getComments(threadId: String, limit: Int, offset: Int): Flow<Result<CommentsResponse>> = apiFlow(
        request = {
            apiService.getComments(
                threadId = threadId,
                body = CommentsRequest(
                    threadId = threadId,
                    rid = threadId.substringAfterLast("_"),
                    limit = limit,
                    offset = offset
                )
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it }
    )

    override fun likeComment(threadId: String, commentId: Long, like: Boolean): Flow<Result<Unit>> = apiFlow(
        request = {
            apiService.likeComment(
                op = if (like) "like" else "unlike",
                body = LikeCommentRequest(
                    threadId = threadId,
                    commentId = commentId
                )
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        msg = { it.message },
        transform = { Unit }
    )
}

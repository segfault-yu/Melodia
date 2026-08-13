package com.lin0721.linmusic.core.comment.data

import com.lin0721.linmusic.core.model.CommentItem
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// 评论相关的网易云 Retrofit 接口定义。
interface CommentApi {

    @POST("/eapi/v1/resource/comments/{threadId}")
    suspend fun getComments(
        @Path("threadId") threadId: String,
        @Body body: CommentsRequest
    ): CommentsResponse

    // 评论点赞与取消点赞
    @POST("/weapi/v1/comment/{op}")
    suspend fun likeComment(
        @Path("op") op: String, // "like" 或 "unlike"
        @Body body: LikeCommentRequest
    ): LikeCommentResponse
}

// ======================= 评论 DTO =======================

@Serializable
data class CommentsRequest(
    val threadId: String,
    val rid: String,
    val limit: Int = 20,
    val offset: Int = 0,
    val beforeTime: Long = 0
)

@Serializable
data class CommentsResponse(
    val code: Int = 0,
    val total: Int = 0,
    val more: Boolean = false,
    val comments: List<CommentItem> = emptyList(),
    val hotComments: List<CommentItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class LikeCommentRequest(
    val threadId: String,
    val commentId: Long
)

@Serializable
data class LikeCommentResponse(
    val code: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}


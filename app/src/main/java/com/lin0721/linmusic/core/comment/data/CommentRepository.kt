package com.lin0721.linmusic.core.comment.data

import kotlinx.coroutines.flow.Flow

// 评论数据仓储（core 共享能力，被 playlist/player 等多个域的评论 Tab 复用）
interface CommentRepository {

    fun getComments(songId: Long, limit: Int = 20, offset: Int = 0): Flow<Result<CommentsResponse>>

    // 获取通用资源的评论 (例如歌单 A_PL_0_ID)
    fun getComments(threadId: String, limit: Int = 20, offset: Int = 0): Flow<Result<CommentsResponse>>

    // 评论点赞/取消点赞
    fun likeComment(threadId: String, commentId: Long, like: Boolean): Flow<Result<Unit>>
}

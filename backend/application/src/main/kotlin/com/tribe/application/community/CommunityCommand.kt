package com.tribe.application.community

object CommunityCommand {
    data class CreatePost(
        val tripId: Long,
        val title: String,
        val content: String,
    )

    data class ListPosts(
        val page: Int = 0,
        val size: Int = 20,
        val country: String? = null,
        val authorId: Long? = null,
    )

    data class GetPostDetail(
        val postId: Long,
    )

    data class UpdatePost(
        val postId: Long,
        val title: String,
        val content: String,
    )

    data class DeletePost(
        val postId: Long,
    )
}

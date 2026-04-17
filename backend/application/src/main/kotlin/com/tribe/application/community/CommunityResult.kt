package com.tribe.application.community

import java.time.LocalDateTime

object CommunityResult {
    data class PostSummary(
        val id: Long,
        val title: String,
        val authorId: Long,
        val authorNickname: String,
        val country: String,
        val representativeImageUrl: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime?,
    )

    data class PostDetail(
        val id: Long,
        val title: String,
        val content: String,
        val authorId: Long,
        val authorNickname: String,
        val country: String,
        val representativeImageUrl: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime?,
    )
}

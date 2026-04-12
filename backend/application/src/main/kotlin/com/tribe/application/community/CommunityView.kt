package com.tribe.application.community

import java.time.LocalDateTime

data class CommunityPostSummary(
    val id: Long,
    val title: String,
    val authorId: Long,
    val authorNickname: String,
    val country: String,
    val representativeImageUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)

data class CommunityPostDetail(
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

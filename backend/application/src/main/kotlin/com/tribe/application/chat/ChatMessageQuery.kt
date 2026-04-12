package com.tribe.application.chat

object ChatMessageQuery {
    data class History(
        val tripId: Long,
        val cursor: String?,
        val pageSize: Int,
    )
}

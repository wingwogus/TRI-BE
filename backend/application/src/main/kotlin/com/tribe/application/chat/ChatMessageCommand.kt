package com.tribe.application.chat

object ChatMessageCommand {
    data class Send(
        val tripId: Long,
        val content: String,
    )
}

package com.tribe.api.chat

object ChatMessageRequests {
    data class SendRequest(
        val content: String,
    )
}

package com.tribe.api.chat

import com.tribe.application.chat.ChatMessageCommand

object ChatMessageRequests {
    data class SendRequest(
        val content: String,
    ) {
        fun toCommand(tripId: Long): ChatMessageCommand.Send = ChatMessageCommand.Send(
            tripId = tripId,
            content = content,
        )
    }
}

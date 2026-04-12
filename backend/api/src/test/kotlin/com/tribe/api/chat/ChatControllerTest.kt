package com.tribe.api.chat

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.chat.ChatMessageCommand
import com.tribe.application.chat.ChatMessageQuery
import com.tribe.application.chat.ChatMessageResult
import com.tribe.application.chat.ChatMessageService
import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ChatController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ChatControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var chatMessageService: ChatMessageService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `sendChat returns message payload`() {
        `when`(chatMessageService.send(ChatMessageCommand.Send(5L, "hello"))).thenReturn(sampleMessage())

        mockMvc.perform(
            post("/api/v1/trips/5/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"hello"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content", equalTo("hello")))
    }

    @Test
    fun `getChatHistory returns paged messages`() {
        `when`(chatMessageService.history(ChatMessageQuery.History(5L, null, 20))).thenReturn(
            ChatMessageResult.History(content = listOf(sampleMessage()), nextCursor = null, hasNext = false)
        )

        mockMvc.perform(get("/api/v1/trips/5/chat"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].content", equalTo("hello")))
    }

    private fun sampleMessage() = ChatMessageResult.Message(
        messageId = 1L,
        sender = ChatMessageResult.Sender(
            tripMemberId = 2L,
            memberId = 1L,
            nickname = "tribe",
            avatar = null,
        ),
        content = "hello",
        timestamp = "2026-04-12T12:00:00",
    )
}

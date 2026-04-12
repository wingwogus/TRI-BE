package com.tribe.api.community

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.community.CommunityPostDetail
import com.tribe.application.community.CommunityPostSummary
import com.tribe.application.community.CommunityQuery
import com.tribe.application.community.CommunityService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDateTime

@WebMvcTest(CommunityController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class CommunityControllerTest(
    @Autowired private val mockMvc: MockMvc
) {

    @MockBean
    private lateinit var communityService: CommunityService

    @MockBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `createPost returns created post`() {
        val now = LocalDateTime.of(2026, 4, 12, 12, 0)
        val requestJson = """{"tripId":1,"title":"title","content":"content"}"""

        `when`(
            communityService.createPost(
                CommunityQuery.CreatePost(1L, "title", "content"),
                null
            )
        ).thenReturn(
            CommunityPostDetail(
                id = 1L,
                title = "title",
                content = "content",
                authorId = 2L,
                authorNickname = "tribe",
                country = "일본",
                representativeImageUrl = null,
                createdAt = now,
                updatedAt = null,
            )
        )

        mockMvc.perform(
            multipart("/api/v1/community/posts")
                .file(MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id", equalTo(1)))
    }

    @Test
    fun `listPosts returns summarized posts`() {
        val now = LocalDateTime.of(2026, 4, 12, 12, 0)

        `when`(communityService.listPosts(CommunityQuery.ListPosts(page = 0, size = 20)))
            .thenReturn(
                listOf(
                    CommunityPostSummary(
                        id = 1L,
                        title = "question",
                        authorId = 2L,
                        authorNickname = "tribe",
                        country = "일본",
                        representativeImageUrl = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            )

        mockMvc.perform(get("/api/v1/community/posts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts[0].id", equalTo(1)))
            .andExpect(jsonPath("$.data.posts[0].authorNickname", equalTo("tribe")))
    }

    @Test
    fun `getPostDetail returns post with comments`() {
        val now = LocalDateTime.of(2026, 4, 12, 12, 0)

        `when`(communityService.getPostDetail(CommunityQuery.GetPostDetail(10L)))
            .thenReturn(
                CommunityPostDetail(
                    id = 10L,
                    title = "review",
                    content = "content",
                    authorId = 1L,
                    authorNickname = "tribe",
                    country = "일본",
                    representativeImageUrl = null,
                    createdAt = now,
                    updatedAt = now,
                )
            )

        mockMvc.perform(get("/api/v1/community/posts/10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", equalTo(10)))
            .andExpect(jsonPath("$.data.authorNickname", equalTo("tribe")))
    }
}

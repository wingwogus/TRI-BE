package com.tribe.api.member

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.member.MemberCommand
import com.tribe.application.member.MemberResult
import com.tribe.application.member.MemberService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MemberController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class MemberControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean
    private lateinit var memberService: MemberService

    @MockBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `getMyProfile returns current member profile`() {
        `when`(memberService.getMyProfile()).thenReturn(sampleProfile())

        mockMvc.perform(get("/api/v1/members/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memberId", equalTo(1)))
            .andExpect(jsonPath("$.data.nickname", equalTo("tribe")))
            .andExpect(jsonPath("$.data.isNewUser", equalTo(true)))
    }

    @Test
    fun `updateNickname returns updated profile`() {
        `when`(memberService.updateNickname(MemberCommand.UpdateNickname("after"))).thenReturn(sampleProfile(nickname = "after"))

        mockMvc.perform(
            patch("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"after"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.nickname", equalTo("after")))
    }

    @Test
    fun `getMemberProfile returns member by id`() {
        `when`(memberService.getMemberProfile(MemberCommand.GetMemberProfile(7L))).thenReturn(sampleProfile(memberId = 7L, nickname = "other"))

        mockMvc.perform(get("/api/v1/members/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.memberId", equalTo(7)))
            .andExpect(jsonPath("$.data.nickname", equalTo("other")))
    }

    @Test
    fun `updateNickname rejects blank nickname`() {
        mockMvc.perform(
            patch("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":" "}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
    }

    private fun sampleProfile(
        memberId: Long = 1L,
        nickname: String = "tribe",
    ) = MemberResult.Profile(
        memberId = memberId,
        nickname = nickname,
        email = "user@example.com",
        avatar = null,
        isNewUser = true,
    )
}

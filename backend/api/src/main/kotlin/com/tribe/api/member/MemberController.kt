package com.tribe.api.member

import com.tribe.api.common.ApiResponse
import com.tribe.application.member.MemberCommand
import com.tribe.application.member.MemberService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
) {
    @GetMapping("/me")
    fun getMyProfile(): ResponseEntity<ApiResponse<MemberResponses.ProfileResponse>> {
        val profile = memberService.getMyProfile()
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.ProfileResponse.from(profile)))
    }

    @PatchMapping("/me")
    fun updateNickname(
        @Valid @RequestBody request: MemberRequests.UpdateNicknameRequest,
    ): ResponseEntity<ApiResponse<MemberResponses.ProfileResponse>> {
        val profile = memberService.updateNickname(MemberCommand.UpdateNickname(request.nickname))
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.ProfileResponse.from(profile)))
    }

    @GetMapping("/{memberId}")
    fun getMemberProfile(
        @PathVariable memberId: Long,
    ): ResponseEntity<ApiResponse<MemberResponses.ProfileResponse>> {
        val profile = memberService.getMemberProfile(MemberCommand.GetMemberProfile(memberId))
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.ProfileResponse.from(profile)))
    }
}

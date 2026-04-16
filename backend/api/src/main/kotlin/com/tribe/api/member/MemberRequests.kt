package com.tribe.api.member

import com.tribe.application.member.MemberCommand
import jakarta.validation.constraints.NotBlank

object MemberRequests {
    data class UpdateNicknameRequest(
        @field:NotBlank(message = "닉네임은 비워둘 수 없습니다.")
        val nickname: String,
    ) {
        fun toCommand(): MemberCommand.UpdateNickname = MemberCommand.UpdateNickname(nickname)
    }
}

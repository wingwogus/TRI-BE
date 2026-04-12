package com.tribe.application.member

object MemberCommand {
    data object GetMyProfile

    data class GetMemberProfile(
        val memberId: Long,
    )

    data class UpdateNickname(
        val nickname: String,
    )
}

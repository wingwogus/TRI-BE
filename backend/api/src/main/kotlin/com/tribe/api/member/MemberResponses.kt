package com.tribe.api.member

import com.tribe.application.member.MemberResult

object MemberResponses {
    data class ProfileResponse(
        val memberId: Long,
        val nickname: String,
        val email: String,
        val avatar: String?,
        val isNewUser: Boolean,
    ) {
        companion object {
            fun from(profile: MemberResult.Profile): ProfileResponse {
                return ProfileResponse(
                    memberId = profile.memberId,
                    nickname = profile.nickname,
                    email = profile.email,
                    avatar = profile.avatar,
                    isNewUser = profile.isNewUser,
                )
            }
        }
    }
}

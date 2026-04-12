package com.tribe.application.member

import com.tribe.domain.member.Member

object MemberResult {
    data class Profile(
        val memberId: Long,
        val nickname: String,
        val email: String,
        val avatar: String?,
        val isNewUser: Boolean,
    ) {
        companion object {
            fun from(member: Member): Profile {
                return Profile(
                    memberId = member.id,
                    nickname = member.nickname,
                    email = member.email,
                    avatar = member.avatar,
                    isNewUser = member.isFirstLogin,
                )
            }
        }
    }
}

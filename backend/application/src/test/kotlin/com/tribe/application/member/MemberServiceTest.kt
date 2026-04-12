package com.tribe.application.member

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var memberRepository: MemberRepository

    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        memberService = MemberService(currentActor, memberRepository)
    }

    @Test
    fun `getMyProfile returns current member and clears first login flag`() {
        val member = Member(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            nickname = "tribe",
            isFirstLogin = true,
        )
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))

        val result = memberService.getMyProfile()

        assertEquals(1L, result.memberId)
        assertEquals("tribe", result.nickname)
        assertEquals(true, result.isNewUser)
        assertFalse(member.isFirstLogin)
    }

    @Test
    fun `getMemberProfile returns requested member without mutating first login flag`() {
        val member = Member(
            id = 2L,
            email = "other@example.com",
            passwordHash = "hashed",
            nickname = "other",
            isFirstLogin = true,
        )
        `when`(memberRepository.findById(2L)).thenReturn(Optional.of(member))

        val result = memberService.getMemberProfile(MemberCommand.GetMemberProfile(2L))

        assertEquals(2L, result.memberId)
        assertEquals("other", result.nickname)
        assertEquals(true, member.isFirstLogin)
    }

    @Test
    fun `updateNickname changes nickname when available`() {
        val member = Member(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            nickname = "before",
        )
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        `when`(memberRepository.existsByNickname("after")).thenReturn(false)

        val result = memberService.updateNickname(MemberCommand.UpdateNickname("after"))

        assertEquals("after", result.nickname)
        assertEquals("after", member.nickname)
    }

    @Test
    fun `updateNickname rejects duplicate nickname`() {
        val member = Member(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            nickname = "before",
        )
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        `when`(memberRepository.existsByNickname("taken")).thenReturn(true)

        val exception = assertThrows(BusinessException::class.java) {
            memberService.updateNickname(MemberCommand.UpdateNickname("taken"))
        }

        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `updateNickname skips duplicate check when nickname is unchanged`() {
        val member = Member(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            nickname = "same",
        )
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))

        val result = memberService.updateNickname(MemberCommand.UpdateNickname("same"))

        assertEquals("same", result.nickname)
        verify(memberRepository, never()).existsByNickname("same")
    }
}

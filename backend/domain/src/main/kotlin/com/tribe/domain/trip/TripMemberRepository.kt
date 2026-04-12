package com.tribe.domain.trip

import com.tribe.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository

interface TripMemberRepository : JpaRepository<TripMember, Long> {
    fun findByTripAndMember(trip: Trip, member: Member): TripMember?
    fun existsByTripIdAndMemberId(tripId: Long, memberId: Long): Boolean
    fun findByTripIdAndMemberId(tripId: Long, memberId: Long): TripMember?
    fun findByTripIdAndRole(tripId: Long, role: TripRole): List<TripMember>
}

package com.tribe.domain.trip.ai

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TripAiProposalRepository : JpaRepository<TripAiProposal, Long> {
    fun findByIdAndRequesterMemberId(id: Long, requesterMemberId: Long): TripAiProposal?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TripAiProposal p where p.id = :id and p.requesterMemberId = :requesterMemberId")
    fun findForUpdateByIdAndRequesterMemberId(
        @Param("id") id: Long,
        @Param("requesterMemberId") requesterMemberId: Long,
    ): TripAiProposal?
}

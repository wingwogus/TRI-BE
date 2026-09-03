package com.tribe.domain.trip.ai

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class TripAiProposal(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TripAiProposalType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TripAiProposalStatus,
    @Column(nullable = false)
    val requesterMemberId: Long,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false)
    val regionCode: String,
    @Column(nullable = false)
    val countryCode: String,
    @Column(nullable = false)
    val startDate: LocalDate,
    @Column(nullable = false)
    val endDate: LocalDate,
    @Column(nullable = false)
    val companionType: String,
    @Column(nullable = false)
    val travelStyles: String,
    @Lob
    @Column(nullable = false)
    val inputSnapshot: String,
    @Lob
    @Column(nullable = false)
    val aiResponseSnapshot: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_ai_proposal_id")
    val id: Long = 0L

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    var appliedAt: LocalDateTime? = null

    var createdTripId: Long? = null

    fun markApplied(tripId: Long) {
        status = TripAiProposalStatus.APPLIED
        createdTripId = tripId
        appliedAt = LocalDateTime.now()
    }
}

enum class TripAiProposalType {
    CREATE_TRIP,
}

enum class TripAiProposalStatus {
    READY,
    APPLIED,
    FAILED,
}

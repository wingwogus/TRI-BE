package com.tribe.application.trip.ai

import java.time.LocalDate

object TripAiGenerationResult {
    data class Proposal(
        val proposalId: Long,
        val status: String,
        val title: String,
        val summary: String?,
        val regionCode: String,
        val country: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val companionType: String,
        val travelStyles: List<String>,
        val warnings: List<String>,
        val days: List<Day>,
        val createdTripId: Long? = null,
    )

    data class Applied(
        val proposalId: Long,
        val status: String,
        val tripId: Long,
        val createdItemCount: Int,
        val unresolvedPlaceCount: Int,
    )

    data class Day(
        val visitDay: Int,
        val theme: String?,
        val items: List<Item>,
    )

    data class Item(
        val order: Int,
        val time: String,
        val placeName: String,
        val searchQuery: String,
        val memo: String?,
        val durationMinutes: Int?,
        val styleTags: List<String>,
    )
}

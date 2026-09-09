package com.tribe.application.trip.ai

import java.time.LocalDate

object TripAiGenerationCommand {
    data class Create(
        val regionCode: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val title: String?,
        val companionType: String,
        val travelStyles: List<String>,
    )

    data class Apply(
        val proposalId: Long,
        val allowTextOnlyItems: Boolean = true,
    )
}

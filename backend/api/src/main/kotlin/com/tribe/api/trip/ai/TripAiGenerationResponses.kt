package com.tribe.api.trip.ai

import com.tribe.application.trip.ai.TripAiGenerationResult
import java.time.LocalDate

object TripAiGenerationResponses {
    data class ProposalResponse(
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
        val createdTripId: Long?,
        val warnings: List<String>,
        val days: List<DayResponse>,
    ) {
        companion object {
            fun from(result: TripAiGenerationResult.Proposal): ProposalResponse = ProposalResponse(
                proposalId = result.proposalId,
                status = result.status,
                title = result.title,
                summary = result.summary,
                regionCode = result.regionCode,
                country = result.country,
                startDate = result.startDate,
                endDate = result.endDate,
                companionType = result.companionType,
                travelStyles = result.travelStyles,
                createdTripId = result.createdTripId,
                warnings = result.warnings,
                days = result.days.map(DayResponse::from),
            )
        }
    }

    data class DayResponse(
        val visitDay: Int,
        val theme: String?,
        val items: List<ItemResponse>,
    ) {
        companion object {
            fun from(result: TripAiGenerationResult.Day): DayResponse = DayResponse(
                visitDay = result.visitDay,
                theme = result.theme,
                items = result.items.map(ItemResponse::from),
            )
        }
    }

    data class ItemResponse(
        val order: Int,
        val time: String,
        val placeName: String,
        val searchQuery: String,
        val memo: String?,
        val durationMinutes: Int?,
        val styleTags: List<String>,
    ) {
        companion object {
            fun from(result: TripAiGenerationResult.Item): ItemResponse = ItemResponse(
                order = result.order,
                time = result.time,
                placeName = result.placeName,
                searchQuery = result.searchQuery,
                memo = result.memo,
                durationMinutes = result.durationMinutes,
                styleTags = result.styleTags,
            )
        }
    }

    data class AppliedResponse(
        val proposalId: Long,
        val status: String,
        val tripId: Long,
        val createdItemCount: Int,
        val unresolvedPlaceCount: Int,
    ) {
        companion object {
            fun from(result: TripAiGenerationResult.Applied): AppliedResponse = AppliedResponse(
                proposalId = result.proposalId,
                status = result.status,
                tripId = result.tripId,
                createdItemCount = result.createdItemCount,
                unresolvedPlaceCount = result.unresolvedPlaceCount,
            )
        }
    }
}

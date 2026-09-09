package com.tribe.application.trip.ai

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceCatalogService
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.core.TripCommand
import com.tribe.application.trip.core.TripService
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.trip.ai.TripAiProposal
import com.tribe.domain.trip.ai.TripAiProposalRepository
import com.tribe.domain.trip.ai.TripAiProposalStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

data class TripAiPlaceCandidate(
    val externalPlaceId: String,
    val placeName: String,
    val address: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
)

data class TripAiResolvedItem(
    val visitDay: Int,
    val order: Int,
    val placeId: Long?,
    val title: String,
    val time: LocalDateTime,
    val memo: String?,
    val placeCandidate: TripAiPlaceCandidate? = null,
) {
    val hasResolvedPlace: Boolean
        get() = placeId != null || placeCandidate != null
}

@Service
@ConditionalOnProperty(name = ["tribe.trip.ai-generation.enabled"], havingValue = "true", matchIfMissing = true)
class TripAiGenerationApplyService(
    private val currentActor: CurrentActor,
    private val proposalRepository: TripAiProposalRepository,
    private val tripService: TripService,
    private val itineraryItemRepository: ItineraryItemRepository,
    private val placeCatalogService: PlaceCatalogService,
) {
    @Transactional
    fun apply(
        proposalId: Long,
        items: List<TripAiResolvedItem>,
        allowTextOnlyItems: Boolean,
    ): TripAiGenerationResult.Applied {
        val requesterId = currentActor.requireUserId()
        val proposal = proposalRepository.findForUpdateByIdAndRequesterMemberId(proposalId, requesterId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

        if (proposal.status == TripAiProposalStatus.APPLIED) {
            return existingResult(proposal)
        }
        if (proposal.status != TripAiProposalStatus.READY) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        if (!allowTextOnlyItems && items.any { !it.hasResolvedPlace }) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND)
        }
        val persistedItems = items.map(::persistPlaceCandidate)

        val trip = tripService.createTripWithItems(
            TripCommand.CreateWithItems(
                title = proposal.title,
                startDate = proposal.startDate,
                endDate = proposal.endDate,
                country = proposal.countryCode,
                regionCode = proposal.regionCode,
                items = persistedItems.map { item ->
                    TripCommand.CreateItem(
                        visitDay = item.visitDay,
                        order = item.order,
                        placeId = item.placeId,
                        title = if (item.placeId == null) item.title else null,
                        time = item.time,
                        memo = item.memo,
                    )
                },
            ),
        )
        proposal.markApplied(trip.tripId)
        return TripAiGenerationResult.Applied(
            proposalId = proposal.id,
            status = proposal.status.name,
            tripId = trip.tripId,
            createdItemCount = persistedItems.size,
            unresolvedPlaceCount = persistedItems.count { it.placeId == null },
        )
    }

    private fun persistPlaceCandidate(item: TripAiResolvedItem): TripAiResolvedItem {
        if (item.placeId != null) return item
        val candidate = item.placeCandidate ?: return item
        val place = placeCatalogService.getOrCreate(
            externalPlaceId = candidate.externalPlaceId,
            placeName = candidate.placeName,
            address = candidate.address,
            latitude = candidate.latitude,
            longitude = candidate.longitude,
        )
        return item.copy(placeId = place.id, placeCandidate = null)
    }

    private fun existingResult(proposal: TripAiProposal): TripAiGenerationResult.Applied {
        val tripId = proposal.createdTripId ?: throw BusinessException(ErrorCode.INVALID_INPUT)
        return TripAiGenerationResult.Applied(
            proposalId = proposal.id,
            status = proposal.status.name,
            tripId = tripId,
            createdItemCount = itineraryItemRepository.countByTripId(tripId),
            unresolvedPlaceCount = itineraryItemRepository.countByTripIdAndPlaceIsNull(tripId),
        )
    }
}

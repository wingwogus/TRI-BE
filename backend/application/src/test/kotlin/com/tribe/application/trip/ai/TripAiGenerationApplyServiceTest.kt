package com.tribe.application.trip.ai

import com.tribe.application.security.CurrentActor
import com.tribe.application.itinerary.place.PlaceCatalogService
import com.tribe.application.trip.core.TripCommand
import com.tribe.application.trip.core.TripResult
import com.tribe.application.trip.core.TripService
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.trip.ai.TripAiProposal
import com.tribe.domain.trip.ai.TripAiProposalRepository
import com.tribe.domain.trip.ai.TripAiProposalStatus
import com.tribe.domain.trip.ai.TripAiProposalType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class TripAiGenerationApplyServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var proposalRepository: TripAiProposalRepository
    @Mock private lateinit var tripService: TripService
    @Mock private lateinit var itineraryItemRepository: ItineraryItemRepository
    @Mock private lateinit var placeCatalogService: PlaceCatalogService

    @Test
    fun `apply creates trip and all items through one bulk command`() {
        val proposal = proposalFixture()
        val items = listOf(
            TripAiResolvedItem(1, 1, 31L, "도톤보리", LocalDateTime.of(2026, 6, 1, 10, 0), "산책"),
            TripAiResolvedItem(1, 2, null, "로컬 식당", LocalDateTime.of(2026, 6, 1, 12, 0), null),
        )
        val expectedCommand = TripCommand.CreateWithItems(
            title = proposal.title,
            startDate = proposal.startDate,
            endDate = proposal.endDate,
            country = proposal.countryCode,
            regionCode = proposal.regionCode,
            items = listOf(
                TripCommand.CreateItem(1, 1, 31L, null, LocalDateTime.of(2026, 6, 1, 10, 0), "산책"),
                TripCommand.CreateItem(1, 2, null, "로컬 식당", LocalDateTime.of(2026, 6, 1, 12, 0), null),
            ),
        )
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findForUpdateByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        `when`(tripService.createTripWithItems(expectedCommand)).thenReturn(tripResult())
        val service = service()

        val result = service.apply(10L, items, allowTextOnlyItems = true)

        assertEquals(77L, result.tripId)
        assertEquals(2, result.createdItemCount)
        assertEquals(1, result.unresolvedPlaceCount)
        assertEquals(TripAiProposalStatus.APPLIED, proposal.status)
        verify(tripService).createTripWithItems(expectedCommand)
        verifyNoInteractions(itineraryItemRepository)
    }

    @Test
    fun `apply retry returns persisted trip and counts without creating another trip`() {
        val proposal = proposalFixture().apply { markApplied(77L) }
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findForUpdateByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        `when`(itineraryItemRepository.countByTripId(77L)).thenReturn(4)
        `when`(itineraryItemRepository.countByTripIdAndPlaceIsNull(77L)).thenReturn(1)
        val service = service()

        val result = service.apply(10L, emptyList(), allowTextOnlyItems = true)

        assertEquals(77L, result.tripId)
        assertEquals(4, result.createdItemCount)
        assertEquals(1, result.unresolvedPlaceCount)
        verifyNoInteractions(tripService)
    }

    @Test
    fun `apply persists searched place candidate only after proposal lock`() {
        val proposal = proposalFixture()
        val candidate = TripAiPlaceCandidate(
            externalPlaceId = "google-31",
            placeName = "도톤보리",
            address = "오사카",
            latitude = BigDecimal("34.6687"),
            longitude = BigDecimal("135.5013"),
        )
        val item = TripAiResolvedItem(
            visitDay = 1,
            order = 1,
            placeId = null,
            title = "도톤보리",
            time = LocalDateTime.of(2026, 6, 1, 10, 0),
            memo = null,
            placeCandidate = candidate,
        )
        val place = Place(
            externalPlaceId = candidate.externalPlaceId,
            name = candidate.placeName,
            address = candidate.address,
            latitude = candidate.latitude,
            longitude = candidate.longitude,
        ).also { ReflectionTestUtils.setField(it, "id", 31L) }
        val expectedCommand = TripCommand.CreateWithItems(
            title = proposal.title,
            startDate = proposal.startDate,
            endDate = proposal.endDate,
            country = proposal.countryCode,
            regionCode = proposal.regionCode,
            items = listOf(
                TripCommand.CreateItem(
                    visitDay = 1,
                    order = 1,
                    placeId = 31L,
                    title = null,
                    time = LocalDateTime.of(2026, 6, 1, 10, 0),
                    memo = null,
                ),
            ),
        )
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findForUpdateByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        `when`(
            placeCatalogService.getOrCreate(
                candidate.externalPlaceId,
                candidate.placeName,
                candidate.address,
                candidate.latitude,
                candidate.longitude,
            ),
        ).thenReturn(place)
        `when`(tripService.createTripWithItems(expectedCommand)).thenReturn(tripResult())
        val service = service()

        val result = service.apply(10L, listOf(item), allowTextOnlyItems = false)

        assertEquals(77L, result.tripId)
        assertEquals(0, result.unresolvedPlaceCount)
        verify(placeCatalogService).getOrCreate(
            candidate.externalPlaceId,
            candidate.placeName,
            candidate.address,
            candidate.latitude,
            candidate.longitude,
        )
        verify(tripService).createTripWithItems(expectedCommand)
    }

    private fun service() = TripAiGenerationApplyService(
        currentActor = currentActor,
        proposalRepository = proposalRepository,
        tripService = tripService,
        itineraryItemRepository = itineraryItemRepository,
        placeCatalogService = placeCatalogService,
    )

    private fun proposalFixture() = TripAiProposal(
        type = TripAiProposalType.CREATE_TRIP,
        status = TripAiProposalStatus.READY,
        requesterMemberId = 1L,
        title = "오사카 가족여행",
        regionCode = "JP_OSAKA_KYOTO",
        countryCode = "JP",
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 1),
        companionType = "FAMILY",
        travelStyles = "FOOD",
        inputSnapshot = "{}",
        aiResponseSnapshot = "{}",
    ).also { ReflectionTestUtils.setField(it, "id", 10L) }

    private fun tripResult() = TripResult.TripDetail(
        tripId = 77L,
        title = "오사카 가족여행",
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 1),
        country = "JP",
        regionCode = "JP_OSAKA_KYOTO",
        members = emptyList(),
    )
}

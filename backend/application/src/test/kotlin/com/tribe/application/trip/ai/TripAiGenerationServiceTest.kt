package com.tribe.application.trip.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceResult
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.security.CurrentActor
import com.tribe.domain.trip.ai.TripAiProposal
import com.tribe.domain.trip.ai.TripAiProposalRepository
import com.tribe.domain.trip.ai.TripAiProposalStatus
import com.tribe.domain.trip.ai.TripAiProposalType
import com.tribe.domain.trip.core.TripRegion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class TripAiGenerationServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var proposalRepository: TripAiProposalRepository
    @Mock private lateinit var placeSearchService: PlaceSearchService
    @Mock private lateinit var applyService: TripAiGenerationApplyService

    private lateinit var service: TripAiGenerationService
    private val objectMapper = jacksonObjectMapper()
    private val parser = TripAiGenerationParser(objectMapper)
    private var geminiResponse: String? = null

    @BeforeEach
    fun setUp() {
        service = TripAiGenerationService(
            currentActor = currentActor,
            geminiGateway = object : GeminiGateway {
                override fun generate(prompt: String): String? = geminiResponse
                override fun generateJson(prompt: String, schema: Map<String, Any>): String? = geminiResponse
            },
            promptBuilder = TripAiGenerationPromptBuilder(objectMapper),
            parser = parser,
            proposalRepository = proposalRepository,
            placeSearchService = placeSearchService,
            applyService = applyService,
        )
    }

    @Test
    fun `createProposal derives country from region and does not create trip`() {
        geminiResponse = validJson()
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.save(any(TripAiProposal::class.java))).thenAnswer { invocation ->
            val proposal = invocation.arguments[0] as TripAiProposal
            ReflectionTestUtils.setField(proposal, "id", 10L)
            proposal
        }

        val result = service.createProposal(createCommand())

        assertEquals(10L, result.proposalId)
        assertEquals("JP", result.country)
        assertEquals(TripRegion.JP_OSAKA_KYOTO.code, result.regionCode)
        assertEquals("READY", result.status)
        verifyNoInteractions(applyService)
    }

    @Test
    fun `createProposal rejects six inclusive days`() {
        val ex = assertThrows(BusinessException::class.java) {
            service.createProposal(
                createCommand(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 6, 6),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
        verifyNoInteractions(proposalRepository, applyService)
    }

    @Test
    fun `applyProposal resolves places then delegates one atomic apply`() {
        val proposal = proposalFixture()
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        stubDotonboriSearchNoResults()
        val expectedItem = TripAiResolvedItem(
            visitDay = 1,
            order = 1,
            placeId = null,
            title = "도톤보리",
            time = java.time.LocalDateTime.of(2026, 6, 1, 10, 0),
            memo = "산책",
        )
        val expectedResult = TripAiGenerationResult.Applied(10L, "APPLIED", 77L, 1, 1)
        `when`(applyService.apply(10L, listOf(expectedItem), true)).thenReturn(expectedResult)

        val result = service.applyProposal(TripAiGenerationCommand.Apply(10L, allowTextOnlyItems = true))

        assertEquals(expectedResult, result)
        verify(applyService).apply(10L, listOf(expectedItem), true)
    }

    @Test
    fun `applyProposal rejects unresolved places before creating trip when text items are disabled`() {
        val proposal = proposalFixture()
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        stubDotonboriSearchNoResults()

        val ex = assertThrows(BusinessException::class.java) {
            service.applyProposal(TripAiGenerationCommand.Apply(10L, allowTextOnlyItems = false))
        }

        assertEquals(ErrorCode.PLACE_NOT_FOUND, ex.errorCode)
        verifyNoInteractions(applyService)
    }

    @Test
    fun `applyProposal returns existing result without resolving places`() {
        val proposal = proposalFixture().apply { markApplied(77L) }
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        val expectedResult = TripAiGenerationResult.Applied(10L, "APPLIED", 77L, 1, 0)
        `when`(applyService.apply(10L, emptyList(), true)).thenReturn(expectedResult)

        val result = service.applyProposal(TripAiGenerationCommand.Apply(10L))

        assertEquals(expectedResult, result)
        verifyNoInteractions(placeSearchService)
    }

    @Test
    fun `applyProposal resolves duplicate canonical search query only once`() {
        val proposal = proposalFixture(duplicateQueryJson())
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(proposalRepository.findByIdAndRequesterMemberId(10L, 1L)).thenReturn(proposal)
        `when`(
            placeSearchService.search(
                "오사카 도톤보리",
                "ko",
                "JP",
                34.6937,
                135.5023,
                50_000,
                "region:JP_OSAKA_KYOTO",
            ),
        ).thenReturn(
            listOf(
                PlaceResult.SearchItem(
                    placeId = 31L,
                    externalPlaceId = "google-31",
                    placeName = "도톤보리",
                    address = "오사카",
                    latitude = 34.6687,
                    longitude = 135.5013,
                ),
            ),
        )
        val expectedItems = listOf(
            TripAiResolvedItem(1, 1, 31L, "도톤보리", java.time.LocalDateTime.of(2026, 6, 1, 10, 0), "산책"),
            TripAiResolvedItem(1, 2, 31L, "도톤보리 재방문", java.time.LocalDateTime.of(2026, 6, 1, 18, 0), null),
        )
        val expectedResult = TripAiGenerationResult.Applied(10L, "APPLIED", 77L, 2, 0)
        `when`(applyService.apply(10L, expectedItems, true)).thenReturn(expectedResult)

        val result = service.applyProposal(TripAiGenerationCommand.Apply(10L))

        assertEquals(expectedResult, result)
        verify(placeSearchService, times(1)).search(
            "오사카 도톤보리",
            "ko",
            "JP",
            34.6937,
            135.5023,
            50_000,
            "region:JP_OSAKA_KYOTO",
        )
    }

    private fun createCommand(
        startDate: LocalDate = LocalDate.of(2026, 6, 1),
        endDate: LocalDate = LocalDate.of(2026, 6, 1),
    ) = TripAiGenerationCommand.Create(
        regionCode = TripRegion.JP_OSAKA_KYOTO.code,
        startDate = startDate,
        endDate = endDate,
        title = "오사카 가족여행",
        companionType = "FAMILY",
        travelStyles = listOf("FOOD", "CULTURE"),
    )

    private fun proposalFixture(json: String = validJson()): TripAiProposal {
        val payload = parser.parse(json, 1)
        return TripAiProposal(
            type = TripAiProposalType.CREATE_TRIP,
            status = TripAiProposalStatus.READY,
            requesterMemberId = 1L,
            title = "오사카 가족여행",
            regionCode = TripRegion.JP_OSAKA_KYOTO.code,
            countryCode = "JP",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 1),
            companionType = "FAMILY",
            travelStyles = "FOOD,CULTURE",
            inputSnapshot = "{}",
            aiResponseSnapshot = parser.toJson(payload),
        ).also {
            ReflectionTestUtils.setField(it, "id", 10L)
        }
    }

    private fun stubDotonboriSearchNoResults() {
        `when`(
            placeSearchService.search(
                "오사카 도톤보리",
                "ko",
                "JP",
                34.6937,
                135.5023,
                50_000,
                "region:JP_OSAKA_KYOTO",
            ),
        ).thenReturn(emptyList())
    }

    private fun validJson() = """
        {
          "title": "오사카 여행",
          "summary": "요약",
          "warnings": [],
          "days": [
            {
              "visitDay": 1,
              "theme": "미식",
              "items": [
                {
                  "order": 1,
                  "time": "10:00",
                  "placeName": "도톤보리",
                  "searchQuery": "오사카 도톤보리",
                  "memo": "산책",
                  "durationMinutes": 90,
                  "styleTags": ["FOOD"]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private fun duplicateQueryJson() = """
        {
          "title": "오사카 여행",
          "days": [
            {
              "visitDay": 1,
              "items": [
                {
                  "order": 1,
                  "time": "10:00",
                  "placeName": "도톤보리",
                  "searchQuery": "오사카 도톤보리",
                  "memo": "산책"
                },
                {
                  "order": 2,
                  "time": "18:00",
                  "placeName": "도톤보리 재방문",
                  "searchQuery": "오사카 도톤보리"
                }
              ]
            }
          ]
        }
    """.trimIndent()
}

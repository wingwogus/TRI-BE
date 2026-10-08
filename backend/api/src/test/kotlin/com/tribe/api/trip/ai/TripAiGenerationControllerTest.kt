package com.tribe.api.trip.ai

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.security.TokenProvider
import com.tribe.application.trip.ai.TripAiGenerationCommand
import com.tribe.application.trip.ai.TripAiGenerationResult
import com.tribe.application.trip.ai.TripAiGenerationService
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(TripAiGenerationController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class TripAiGenerationControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean
    private lateinit var tripAiGenerationService: TripAiGenerationService

    @MockBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `createProposal accepts valid request`() {
        `when`(
            tripAiGenerationService.createProposal(
                TripAiGenerationCommand.Create(
                    regionCode = "JP_OSAKA_KYOTO",
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 6, 5),
                    title = "오사카 가족여행",
                    companionType = "FAMILY",
                    travelStyles = listOf("FOOD", "CULTURE"),
                ),
            ),
        ).thenReturn(sampleProposal())

        mockMvc.perform(
            post("/api/v1/trips/ai-generations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "regionCode": "JP_OSAKA_KYOTO",
                      "startDate": "2026-06-01",
                      "endDate": "2026-06-05",
                      "title": "오사카 가족여행",
                      "companionType": "FAMILY",
                      "travelStyles": ["FOOD", "CULTURE"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.proposalId", equalTo(10)))
            .andExpect(jsonPath("$.data.regionCode", equalTo("JP_OSAKA_KYOTO")))
            .andExpect(jsonPath("$.data.days[0].items[0].placeName", equalTo("도톤보리")))
    }

    @Test
    fun `createProposal rejects six inclusive days`() {
        mockMvc.perform(
            post("/api/v1/trips/ai-generations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "regionCode": "JP_OSAKA_KYOTO",
                      "startDate": "2026-06-01",
                      "endDate": "2026-06-06",
                      "companionType": "FAMILY",
                      "travelStyles": ["FOOD"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `getProposal returns preview`() {
        `when`(tripAiGenerationService.getProposal(10L)).thenReturn(sampleProposal())

        mockMvc.perform(get("/api/v1/trips/ai-generations/10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.proposalId", equalTo(10)))
            .andExpect(jsonPath("$.data.createdTripId").doesNotExist())
    }

    @Test
    fun `getProposal exposes created trip id after apply`() {
        `when`(tripAiGenerationService.getProposal(10L)).thenReturn(
            sampleProposal().copy(status = "APPLIED", createdTripId = 77L),
        )

        mockMvc.perform(get("/api/v1/trips/ai-generations/10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status", equalTo("APPLIED")))
            .andExpect(jsonPath("$.data.createdTripId", equalTo(77)))
    }

    @Test
    fun `applyProposal maps request`() {
        `when`(
            tripAiGenerationService.applyProposal(TripAiGenerationCommand.Apply(10L, allowTextOnlyItems = false)),
        ).thenReturn(
            TripAiGenerationResult.Applied(
                proposalId = 10L,
                status = "APPLIED",
                tripId = 77L,
                createdItemCount = 3,
                unresolvedPlaceCount = 0,
            ),
        )

        mockMvc.perform(
            post("/api/v1/trips/ai-generations/10/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"allowTextOnlyItems":false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status", equalTo("APPLIED")))
            .andExpect(jsonPath("$.data.tripId", equalTo(77)))
    }

    private fun sampleProposal() = TripAiGenerationResult.Proposal(
        proposalId = 10L,
        status = "READY",
        title = "오사카 가족여행",
        summary = "요약",
        regionCode = "JP_OSAKA_KYOTO",
        country = "JP",
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 5),
        companionType = "FAMILY",
        travelStyles = listOf("FOOD", "CULTURE"),
        warnings = emptyList(),
        days = listOf(
            TripAiGenerationResult.Day(
                visitDay = 1,
                theme = "미식",
                items = listOf(
                    TripAiGenerationResult.Item(
                        order = 1,
                        time = "10:00",
                        placeName = "도톤보리",
                        searchQuery = "오사카 도톤보리",
                        memo = "산책",
                        durationMinutes = 90,
                        styleTags = listOf("FOOD"),
                    ),
                ),
            ),
        ),
    )
}

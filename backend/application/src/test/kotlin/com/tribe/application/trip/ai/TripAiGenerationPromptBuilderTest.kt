package com.tribe.application.trip.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.trip.core.TripRegion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TripAiGenerationPromptBuilderTest {
    private val builder = TripAiGenerationPromptBuilder(jacksonObjectMapper())

    @Test
    fun `builds normalized prompt from trip region`() {
        val prompt = builder.build(
            region = TripRegion.JP_OSAKA_KYOTO,
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 5),
            dayCount = 5,
            companionType = TripCompanionType.FAMILY,
            travelStyles = listOf(TripTravelStyle.FOOD, TripTravelStyle.CULTURE),
        )

        assertTrue(prompt.contains("JP_OSAKA_KYOTO"))
        assertTrue(prompt.contains("오사카/교토"))
        assertTrue(prompt.contains("FAMILY"))
        assertTrue(prompt.contains("FOOD"))
        assertTrue(prompt.contains("\"dayCount\":5"))
    }
}

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
        assertTrue(prompt.contains("\"minItemsPerDay\":5"))
        assertTrue(prompt.contains("아침 식사/오전 활동/점심 식사/오후 활동"))
        assertTrue(prompt.contains("이동이 대중교통 또는 도보 기준 45분을 넘지 않도록"))
        assertTrue(prompt.contains("그 장소가 어떤 곳인지, 무엇을 할 수 있는지, 왜 이 일정에 추천했는지"))
    }
}

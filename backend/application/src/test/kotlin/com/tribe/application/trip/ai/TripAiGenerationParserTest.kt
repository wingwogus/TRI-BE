package com.tribe.application.trip.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.application.exception.business.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TripAiGenerationParserTest {
    private val parser = TripAiGenerationParser(jacksonObjectMapper())

    @Test
    fun `parses valid ai output`() {
        val result = parser.parse(validJson(), 1)

        assertEquals("오사카 여행", result.title)
        assertEquals(1, result.days.size)
        assertEquals("도톤보리", result.days.first().items.first().placeName)
    }

    @Test
    fun `strips fenced json defensively`() {
        val result = parser.parse("```json\n${validJson()}\n```", 1)

        assertEquals("오사카 여행", result.title)
    }

    @Test
    fun `rejects day count mismatch`() {
        assertThrows(BusinessException::class.java) {
            parser.parse(validJson(), 2)
        }
    }

    @Test
    fun `rejects invalid time format`() {
        val invalid = validJson().replace("10:00", "25:00")

        assertThrows(BusinessException::class.java) {
            parser.parse(invalid, 1)
        }
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
}

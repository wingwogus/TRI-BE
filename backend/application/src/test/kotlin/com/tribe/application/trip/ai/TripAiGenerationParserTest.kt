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
        val invalid = validJson().replace("08:30", "25:00")

        assertThrows(BusinessException::class.java) {
            parser.parse(invalid, 1)
        }
    }

    @Test
    fun `rejects sparse days with fewer than five places`() {
        val sparse = """
            {"title":"오사카","days":[{"visitDay":1,"items":[
              {"order":1,"time":"08:00","placeName":"아침","searchQuery":"오사카 아침"},
              {"order":2,"time":"10:00","placeName":"오전","searchQuery":"오사카 오전"},
              {"order":3,"time":"12:00","placeName":"점심","searchQuery":"오사카 점심"},
              {"order":4,"time":"18:00","placeName":"저녁","searchQuery":"오사카 저녁"}
            ]}]}
        """.trimIndent()

        assertThrows(BusinessException::class.java) {
            parser.parse(sparse, 1)
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
                  "time": "08:30",
                  "placeName": "도톤보리",
                  "searchQuery": "오사카 난바 도톤보리 아침 식당",
                  "memo": "아침 식사",
                  "durationMinutes": 90,
                  "styleTags": ["FOOD"]
                },
                {
                  "order": 2,
                  "time": "10:30",
                  "placeName": "도톤보리",
                  "searchQuery": "오사카 도톤보리",
                  "memo": "산책",
                  "durationMinutes": 90,
                  "styleTags": ["CULTURE"]
                },
                {
                  "order": 3,
                  "time": "12:30",
                  "placeName": "난바 점심 식당",
                  "searchQuery": "오사카 난바 점심 식당",
                  "memo": "점심 식사",
                  "durationMinutes": 60,
                  "styleTags": ["FOOD"]
                },
                {
                  "order": 4,
                  "time": "15:00",
                  "placeName": "난바 카페",
                  "searchQuery": "오사카 난바 카페",
                  "memo": "휴식",
                  "durationMinutes": 90,
                  "styleTags": ["HEALING"]
                },
                {
                  "order": 5,
                  "time": "18:30",
                  "placeName": "난바 저녁 식당",
                  "searchQuery": "오사카 난바 저녁 식당",
                  "memo": "저녁 식사",
                  "durationMinutes": 90,
                  "styleTags": ["FOOD"]
                }
              ]
            }
          ]
        }
    """.trimIndent()
}

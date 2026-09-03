package com.tribe.application.trip.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.domain.trip.core.TripRegion
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TripAiGenerationPromptBuilder(
    private val objectMapper: ObjectMapper,
) {
    fun build(
        region: TripRegion,
        startDate: LocalDate,
        endDate: LocalDate,
        dayCount: Int,
        companionType: TripCompanionType,
        travelStyles: List<TripTravelStyle>,
    ): String {
        val input = mapOf(
            "region" to mapOf(
                "code" to region.code,
                "label" to region.label,
                "countryCode" to region.country.code,
                "countryName" to region.country.koreanName,
                "type" to region.type.name,
                "centerLat" to region.centerLat,
                "centerLng" to region.centerLng,
                "searchHints" to region.searchHints,
            ),
            "trip" to mapOf(
                "startDate" to startDate.toString(),
                "endDate" to endDate.toString(),
                "dayCount" to dayCount,
                "maxItemsPerDay" to MAX_ITEMS_PER_DAY,
            ),
            "preferences" to mapOf(
                "companionType" to companionType.name,
                "travelStyles" to travelStyles.map { it.name },
            ),
        )

        return """
        당신은 여행 일정 생성 엔진입니다.
        아래 입력 JSON만 근거로 사용해 여행 일정을 생성하세요.

        규칙:
        1. 출력은 제공된 JSON schema를 만족하는 JSON object만 반환하세요.
        2. markdown, 설명 문장, 코드블록을 출력하지 마세요.
        3. 모든 장소는 companionType과 travelStyles에 맞춰 선택하세요
        4. 모든 장소는 region.code, region.label, region.searchHints와 관련된 장소만 선택하세요.
        5. days 배열 개수는 trip.dayCount와 정확히 같아야 합니다.
        6. 각 day는 1개 이상 ${MAX_ITEMS_PER_DAY}개 이하의 item만 포함하세요.
        7. time은 HH:mm 형식으로 출력하세요.
        8. companionType과 travelStyles에 맞춰 무리 없는 동선을 구성하세요.
        9. searchQuery는 Google Places 검색에 적합하도록 지역명과 장소명을 함께 구체적으로 작성하세요.

        입력 JSON:
        ${objectMapper.writeValueAsString(input)}
        """.trimIndent()
    }

    fun normalizedInput(
        region: TripRegion,
        startDate: LocalDate,
        endDate: LocalDate,
        dayCount: Int,
        companionType: TripCompanionType,
        travelStyles: List<TripTravelStyle>,
    ): String {
        val input = mapOf(
            "regionCode" to region.code,
            "countryCode" to region.country.code,
            "regionLabel" to region.label,
            "startDate" to startDate.toString(),
            "endDate" to endDate.toString(),
            "dayCount" to dayCount,
            "companionType" to companionType.name,
            "travelStyles" to travelStyles.map { it.name },
        )
        return objectMapper.writeValueAsString(input)
    }

    companion object {
        const val MAX_ITEMS_PER_DAY = 4
    }
}

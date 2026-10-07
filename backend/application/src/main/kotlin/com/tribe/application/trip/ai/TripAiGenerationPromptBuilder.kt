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
                "minItemsPerDay" to MIN_ITEMS_PER_DAY,
                "maxItemsPerDay" to MAX_ITEMS_PER_DAY,
                "recommendedItemsPerDay" to recommendedItemsPerDay(companionType, travelStyles),
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
        6. 각 day는 반드시 ${MIN_ITEMS_PER_DAY}개 이상 ${MAX_ITEMS_PER_DAY}개 이하의 item을 포함하세요. 하루를 한두 곳으로 끝내지 말고 아침부터 저녁까지 풀코스로 구성하세요. 입력의 recommendedItemsPerDay를 일반적인 하루의 정확한 목표 개수로 사용하세요. 별도 도착·출발 정보가 없으므로 임의로 5개로 줄이지 말고, 권장값이 6이면 반드시 6개를 채우세요.
        7. 각 day의 item은 가능하면 아침 식사/오전 활동/점심 식사/오후 활동 또는 카페/저녁 식사 순서로 배치하세요. 식사 장소도 실제 방문 place로 포함하세요.
        8. time은 HH:mm 형식으로 출력하고, 아침은 08:00~10:00, 점심은 11:30~13:30, 저녁은 17:30~20:30 사이에 배치하세요.
        9. 하루의 장소는 같은 생활권이나 인접한 동네를 묶어 구성하고, 연속한 장소 사이 이동이 대중교통 또는 도보 기준 45분을 넘지 않도록 하세요. 먼 지역을 하루에 왕복하거나 동선을 되짚지 마세요.
        10. companionType과 travelStyles에 맞춰 무리 없는 체류 시간과 이동 시간을 반영하세요.
        11. 모든 item의 memo는 반드시 작성하세요. 단순히 '점심'이나 '산책'이라고 쓰지 말고, 그 장소가 어떤 곳인지, 무엇을 할 수 있는지, 왜 이 일정에 추천했는지를 한국어 한두 문장으로 설명하세요.
        12. searchQuery는 Google Places 검색에 적합하도록 지역명, 동네명, 장소명을 함께 구체적으로 작성하세요.

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

    fun recommendedItemsPerDay(
        companionType: TripCompanionType,
        travelStyles: List<TripTravelStyle>,
    ): Int {
        val denseStyles = setOf(
            TripTravelStyle.FOOD,
            TripTravelStyle.ACTIVITY,
            TripTravelStyle.SHOPPING,
            TripTravelStyle.PHOTO,
            TripTravelStyle.LOCAL,
        )
        val energeticCompanions = setOf(TripCompanionType.COUPLE, TripCompanionType.FRIENDS)
        val isRelaxed = companionType == TripCompanionType.SOLO || companionType == TripCompanionType.PARENTS
        return if (!isRelaxed && (travelStyles.any(denseStyles::contains) || companionType in energeticCompanions)) 6 else 5
    }

    companion object {
        const val MIN_ITEMS_PER_DAY = 5
        const val MAX_ITEMS_PER_DAY = 6
    }
}

package com.tribe.application.itinerary.place

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceResultAssemblerCategoryTest {
    @Test
    fun `normalize prefers cuisine specific restaurant types`() {
        assertEquals(
            NormalizedPlaceCategoryKey.KOREAN_FOOD,
            PlaceResultAssembler.normalizeCategory("korean_restaurant", listOf("korean_restaurant", "restaurant", "food")),
        )
        assertEquals(
            NormalizedPlaceCategoryKey.JAPANESE_FOOD,
            PlaceResultAssembler.normalizeCategory("restaurant", listOf("restaurant", "ramen_restaurant", "food")),
        )
    }

    @Test
    fun `normalize maps generic restaurant to restaurant bucket`() {
        assertEquals(
            NormalizedPlaceCategoryKey.RESTAURANT,
            PlaceResultAssembler.normalizeCategory("restaurant", listOf("restaurant", "food")),
        )
    }

    @Test
    fun `normalize maps attraction and transport types`() {
        assertEquals(
            NormalizedPlaceCategoryKey.ATTRACTION,
            PlaceResultAssembler.normalizeCategory("tourist_attraction", listOf("tourist_attraction", "point_of_interest")),
        )
        assertEquals(
            NormalizedPlaceCategoryKey.TRANSPORT,
            PlaceResultAssembler.normalizeCategory("airport", listOf("airport", "point_of_interest")),
        )
    }

    @Test
    fun `normalize falls back to etc for unknown types and null for empty inputs`() {
        assertEquals(
            NormalizedPlaceCategoryKey.ETC,
            PlaceResultAssembler.normalizeCategory("spa", listOf("spa", "point_of_interest")),
        )
        assertEquals(
            null,
            PlaceResultAssembler.normalizeCategory(null, emptyList()),
        )
    }
}

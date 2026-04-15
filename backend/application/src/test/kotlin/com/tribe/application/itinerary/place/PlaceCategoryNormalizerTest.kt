package com.tribe.application.itinerary.place

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceCategoryNormalizerTest {
    @Test
    fun `normalize prefers cuisine specific restaurant types`() {
        assertEquals(
            NormalizedPlaceCategoryKey.KOREAN_FOOD,
            PlaceCategoryNormalizer.normalize("korean_restaurant", listOf("korean_restaurant", "restaurant", "food")),
        )
        assertEquals(
            NormalizedPlaceCategoryKey.JAPANESE_FOOD,
            PlaceCategoryNormalizer.normalize("restaurant", listOf("restaurant", "ramen_restaurant", "food")),
        )
    }

    @Test
    fun `normalize maps generic restaurant to restaurant bucket`() {
        assertEquals(
            NormalizedPlaceCategoryKey.RESTAURANT,
            PlaceCategoryNormalizer.normalize("restaurant", listOf("restaurant", "food")),
        )
    }

    @Test
    fun `normalize maps attraction and transport types`() {
        assertEquals(
            NormalizedPlaceCategoryKey.ATTRACTION,
            PlaceCategoryNormalizer.normalize("tourist_attraction", listOf("tourist_attraction", "point_of_interest")),
        )
        assertEquals(
            NormalizedPlaceCategoryKey.TRANSPORT,
            PlaceCategoryNormalizer.normalize("airport", listOf("airport", "point_of_interest")),
        )
    }

    @Test
    fun `normalize falls back to etc for unknown types and null for empty inputs`() {
        assertEquals(
            NormalizedPlaceCategoryKey.ETC,
            PlaceCategoryNormalizer.normalize("spa", listOf("spa", "point_of_interest")),
        )
        assertEquals(
            null,
            PlaceCategoryNormalizer.normalize(null, emptyList()),
        )
    }
}

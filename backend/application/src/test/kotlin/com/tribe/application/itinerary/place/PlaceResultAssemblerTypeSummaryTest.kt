package com.tribe.application.itinerary.place

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlaceResultAssemblerTypeSummaryTest {
    @Test
    fun `fromRawTypes returns summary with display label`() {
        val result = PlaceResultAssembler.fromRawTypes(
            primaryType = "korean_restaurant",
            types = listOf("korean_restaurant", "restaurant"),
        )

        requireNotNull(result)
        assertEquals("korean_restaurant", result.primaryType)
        assertEquals(listOf("korean_restaurant", "restaurant"), result.types)
        assertEquals("korean restaurant", result.displayPrimaryLabel)
    }

    @Test
    fun `fromGoogleTypesJson decodes types payload`() {
        val result = PlaceResultAssembler.fromGoogleTypesJson(
            primaryType = "cafe",
            googleTypesJson = "[\"cafe\",\"food\"]",
        )

        requireNotNull(result)
        assertEquals(listOf("cafe", "food"), result.types)
    }

    @Test
    fun `fromRawTypes returns null when no primary type and no types`() {
        assertNull(PlaceResultAssembler.fromRawTypes(null, emptyList()))
    }
}

package com.tribe.api.itinerary.place

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.itinerary.place.PlaceSearchContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class GooglePlaceSearchGatewayTest {
    private val gateway = GooglePlaceSearchGateway(
        webClientBuilder = WebClient.builder(),
        objectMapper = ObjectMapper(),
        apiKey = "test-key",
    )

    @Test
    fun `buildSearchRequestBody returns null for blank query`() {
        val body = gateway.buildSearchRequestBody(
            query = "   ",
            language = "ko",
            context = PlaceSearchContext(regionCode = "JP"),
        )

        assertNull(body)
    }

    @Test
    fun `buildSearchRequestBody normalizes region and clamps radius`() {
        val body = gateway.buildSearchRequestBody(
            query = " tower ",
            language = "ko",
            context = PlaceSearchContext(
                regionCode = "jp ",
                latitude = 35.0,
                longitude = 139.0,
                radiusMeters = 500_000,
            ),
        )

        requireNotNull(body)
        assertEquals("tower", body["textQuery"])
        assertEquals("JP", body["regionCode"])
        val locationBias = body["locationBias"] as Map<*, *>
        val circle = locationBias["circle"] as Map<*, *>
        assertEquals(50_000, circle["radius"])
    }

    @Test
    fun `buildSearchRequestBody omits invalid region code`() {
        val body = gateway.buildSearchRequestBody(
            query = "tower",
            language = "ko",
            context = PlaceSearchContext(regionCode = "JPN"),
        )

        requireNotNull(body)
        assertFalse(body.containsKey("regionCode"))
    }

    @Test
    fun `parsePriceLevel maps google enum strings to numeric levels`() {
        assertEquals(2, gateway.parsePriceLevel("PRICE_LEVEL_MODERATE"))
        assertEquals(4, gateway.parsePriceLevel("PRICE_LEVEL_VERY_EXPENSIVE"))
        assertEquals(null, gateway.parsePriceLevel("PRICE_LEVEL_UNSPECIFIED"))
        assertEquals(null, gateway.parsePriceLevel("UNKNOWN"))
    }
}

package com.tribe.application.trip.review

import com.tribe.application.itinerary.place.PlaceSearchGateway
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class TripRouteOptimizationServiceTest {
    @Mock private lateinit var placeSearchService: PlaceSearchService

    @Test
    fun `preview returns optimized route per visit day`() {
        val service = TripRouteOptimizationService(placeSearchService)
        val trip = Trip("Route", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), Country.JAPAN)
        val far = place("far", "Far", 35.0, 135.0, 1L)
        val near = place("near", "Near", 35.1, 135.1, 2L)
        val middle = place("middle", "Middle", 35.2, 135.2, 3L)
        val item1 = item(trip, 11L, far, 1)
        val item2 = item(trip, 12L, middle, 2)
        val item3 = item(trip, 13L, near, 3)
        trip.itineraryItems.addAll(listOf(item1, item2, item3))

        `when`(placeSearchService.directions("far", "middle", "WALKING")).thenReturn(route("far", "middle", 100))
        `when`(placeSearchService.directions("far", "near", "WALKING")).thenReturn(route("far", "near", 10))
        `when`(placeSearchService.directions("near", "middle", "WALKING")).thenReturn(route("near", "middle", 10))
        `when`(placeSearchService.directions("middle", "near", "WALKING")).thenReturn(route("middle", "near", 100))

        val result = service.preview(trip)
        val day = result.days.single()

        assertEquals(listOf(11L, 12L, 13L), day.currentOrder.map { it.itemId })
        assertEquals(listOf(11L, 13L, 12L), day.optimizedOrder.map { it.itemId })
        assertEquals(200, day.currentDurationSeconds)
        assertEquals(20, day.optimizedDurationSeconds)
        assertEquals(180, day.savedDurationSeconds)
    }

    @Test
    fun `preview keeps text only item position and warns`() {
        val service = TripRouteOptimizationService(placeSearchService)
        val trip = Trip("Route", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), Country.JAPAN)
        val origin = place("origin", "Origin", 35.0, 135.0, 1L)
        val destination = place("dest", "Destination", 35.1, 135.1, 2L)
        val item1 = item(trip, 11L, origin, 1)
        val textItem = ItineraryItem(trip, 1, null, "Break", null, 2, null)
        ReflectionTestUtils.setField(textItem, "id", 12L)
        val item3 = item(trip, 13L, destination, 3)
        trip.itineraryItems.addAll(listOf(item1, textItem, item3))

        val day = service.preview(trip).days.single()

        assertEquals(listOf(11L, 12L, 13L), day.optimizedOrder.map { it.itemId })
        assertEquals(0, day.optimizedDurationSeconds)
        assertEquals(true, day.warnings.any { it.contains("장소 정보가 없는 일정") })
    }

    private fun place(externalPlaceId: String, name: String, latitude: Double, longitude: Double, id: Long): Place {
        val place = Place(
            externalPlaceId = externalPlaceId,
            name = name,
            address = "address",
            latitude = BigDecimal.valueOf(latitude),
            longitude = BigDecimal.valueOf(longitude),
        )
        ReflectionTestUtils.setField(place, "id", id)
        return place
    }

    private fun item(trip: Trip, id: Long, place: Place, order: Int): ItineraryItem {
        val item = ItineraryItem(trip, 1, place, null, null, order, null)
        ReflectionTestUtils.setField(item, "id", id)
        return item
    }

    private fun route(origin: String, destination: String, durationSeconds: Long): RouteDetails =
        RouteDetails(
            travelMode = "WALKING",
            originPlace = PlaceSearchGateway.SearchHit(origin, origin, "address", 0.0, 0.0),
            destinationPlace = PlaceSearchGateway.SearchHit(destination, destination, "address", 0.0, 0.0),
            totalDuration = "$durationSeconds sec",
            totalDistance = "1 m",
            totalDurationSeconds = durationSeconds,
            totalDistanceMeters = 1,
            steps = emptyList(),
        )
}

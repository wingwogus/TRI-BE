package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class OpeningHoursEvaluatorTest {
    private val evaluator = OpeningHoursEvaluator()

    @Test
    fun `returns none when visit time is within regular opening period`() {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 15), Country.JAPAN)
        val place = Place("place-1", "Cafe", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        place.regularOpeningPeriods.add(
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = 1,
                openMinute = 9 * 60,
                closeMinute = 18 * 60,
                isOvernight = false,
                sequenceNo = 1,
            ),
        )
        val item = ItineraryItem(trip, 1, place, null, LocalDateTime.of(2026, 4, 13, 10, 0), 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals("OPEN", result)
    }

    @Test
    fun `returns outside business hours when visit is outside period`() {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 15), Country.JAPAN)
        val place = Place("place-1", "Cafe", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        place.regularOpeningPeriods.add(
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = 1,
                openMinute = 9 * 60,
                closeMinute = 18 * 60,
                isOvernight = false,
                sequenceNo = 1,
            ),
        )
        val item = ItineraryItem(trip, 1, place, null, LocalDateTime.of(2026, 4, 13, 20, 0), 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals("OUTSIDE_BUSINESS_HOURS", result)
    }
}

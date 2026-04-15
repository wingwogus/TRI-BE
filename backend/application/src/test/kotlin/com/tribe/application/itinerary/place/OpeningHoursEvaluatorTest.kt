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

    @Test
    fun `returns closed day possible when time is missing and place is closed that day`() {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 15), Country.JAPAN)
        val place = Place("place-1", "Cafe", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        place.regularOpeningPeriods.add(
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = 2,
                openMinute = 9 * 60,
                closeMinute = 18 * 60,
                isOvernight = false,
                sequenceNo = 1,
            ),
        )
        val item = ItineraryItem(trip, 1, place, null, null, 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals("CLOSED_DAY_POSSIBLE", result)
    }

    @Test
    fun `returns null when time is missing and place has hours on that day`() {
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
        val item = ItineraryItem(trip, 1, place, null, null, 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals(null, result)
    }

    @Test
    fun `returns closed day possible when only previous day overnight period exists and time is missing`() {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 15), Country.JAPAN)
        val place = Place("place-1", "Bar", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        place.regularOpeningPeriods.add(
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = 0,
                openMinute = 18 * 60,
                closeMinute = 2 * 60,
                isOvernight = true,
                sequenceNo = 1,
            ),
        )
        val item = ItineraryItem(trip, 1, place, null, null, 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals("CLOSED_DAY_POSSIBLE", result)
    }

    @Test
    fun `returns temporarily closed even when time is missing`() {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 15), Country.JAPAN)
        val place = Place("place-1", "Cafe", "Tokyo", BigDecimal.ONE, BigDecimal.TEN).apply {
            businessStatus = "CLOSED_TEMPORARILY"
        }
        val item = ItineraryItem(trip, 1, place, null, null, 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals("TEMPORARILY_CLOSED", result)
    }

    @Test
    fun `returns null when regular opening periods are unavailable`() {
        val trip = Trip("Trip", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 15), Country.JAPAN)
        val place = Place("place-1", "Cafe", "Tokyo", BigDecimal.ONE, BigDecimal.TEN)
        val item = ItineraryItem(trip, 1, place, null, null, 1, null)

        val result = evaluator.evaluate(item, trip.startDate)

        assertEquals(null, result)
    }
}

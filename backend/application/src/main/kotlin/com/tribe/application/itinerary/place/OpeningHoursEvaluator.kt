package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OpeningHoursEvaluator {
    fun evaluate(item: ItineraryItem, tripStartDate: LocalDate): String? {
        val place = item.place ?: return null
        if (place.businessStatus == "CLOSED_TEMPORARILY") return "TEMPORARILY_CLOSED"
        val visitDate = tripStartDate.plusDays((item.visitDay - 1).toLong())
        val dayOfWeek = toGoogleDayOfWeek(visitDate)
        val previousDay = (dayOfWeek + 6) % 7
        if (place.regularOpeningPeriods.isEmpty()) return null

        val todayPeriods = place.regularOpeningPeriods.filter { it.dayOfWeek == dayOfWeek }
        val previousDayOvernightPeriods = place.regularOpeningPeriods.filter { it.dayOfWeek == previousDay && it.isOvernight }

        val visitTime = item.time ?: return if (todayPeriods.isEmpty()) "CLOSED_DAY_POSSIBLE" else null

        val minuteOfDay = visitTime.hour * 60 + visitTime.minute

        if (todayPeriods.isEmpty() && previousDayOvernightPeriods.isEmpty()) return "CLOSED_DAY"
        if (todayPeriods.any { isWithinPeriod(it, minuteOfDay) }) return "OPEN"
        if (previousDayOvernightPeriods.any { minuteOfDay < it.closeMinute }) return "OPEN"
        return "OUTSIDE_BUSINESS_HOURS"
    }

    private fun toGoogleDayOfWeek(value: LocalDate): Int = value.dayOfWeek.value % 7

    private fun isWithinPeriod(period: PlaceRegularOpeningPeriod, minuteOfDay: Int): Boolean {
        return if (period.isOvernight) {
            minuteOfDay >= period.openMinute
        } else {
            minuteOfDay in period.openMinute until period.closeMinute
        }
    }
}

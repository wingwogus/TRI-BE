package com.tribe.domain.itinerary.place

import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRegularOpeningPeriodRepository : JpaRepository<PlaceRegularOpeningPeriod, Long> {
    fun deleteAllByPlaceId(placeId: Long)
    fun findAllByPlaceIdOrderByDayOfWeekAscSequenceNoAsc(placeId: Long): List<PlaceRegularOpeningPeriod>
}

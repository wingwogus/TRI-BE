package com.tribe.domain.itinerary

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItineraryItemRepository : JpaRepository<ItineraryItem, Long> {
    fun findByCategoryIdOrderByOrderAsc(categoryId: Long): List<ItineraryItem>
    fun countByCategoryId(categoryId: Long): Int

    @Query("select i from ItineraryItem i join i.category c where i.id in :itemIds and c.trip.id = :tripId")
    fun findByIdInAndTripId(@Param("itemIds") itemIds: List<Long>, @Param("tripId") tripId: Long): List<ItineraryItem>
}

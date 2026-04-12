package com.tribe.application.itinerary

import com.tribe.domain.itinerary.ItineraryItem
import java.time.LocalDateTime

object ItemResult {
    data class ItemView(
        val itemId: Long,
        val categoryId: Long,
        val categoryName: String,
        val tripId: Long,
        val day: Int,
        val title: String?,
        val time: LocalDateTime?,
        val order: Int,
        val memo: String?,
    ) {
        companion object {
            fun from(item: ItineraryItem) = ItemView(
                itemId = item.id,
                categoryId = item.category.id,
                categoryName = item.category.name,
                tripId = item.category.trip.id,
                day = item.category.day,
                title = item.title,
                time = item.time,
                order = item.order,
                memo = item.memo,
            )
        }
    }
}

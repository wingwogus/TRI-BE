package com.tribe.api.itinerary

import com.tribe.application.itinerary.ItemResult
import java.time.LocalDateTime

object ItemResponses {
    data class ItemResponse(
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
            fun from(view: ItemResult.ItemView) = ItemResponse(
                itemId = view.itemId,
                categoryId = view.categoryId,
                categoryName = view.categoryName,
                tripId = view.tripId,
                day = view.day,
                title = view.title,
                time = view.time,
                order = view.order,
                memo = view.memo,
            )
        }
    }
}

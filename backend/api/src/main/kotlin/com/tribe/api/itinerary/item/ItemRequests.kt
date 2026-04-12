package com.tribe.api.itinerary.item

import java.time.LocalDateTime

object ItemRequests {
    data class CreateRequest(
        val categoryId: Long,
        val placeId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class UpdateRequest(
        val categoryId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class OrderUpdateRequest(
        val items: List<OrderItemRequest>,
    )

    data class OrderItemRequest(
        val itemId: Long,
        val categoryId: Long,
        val order: Int,
    )
}

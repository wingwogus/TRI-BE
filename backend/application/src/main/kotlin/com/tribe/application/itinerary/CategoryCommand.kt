package com.tribe.application.itinerary

object CategoryCommand {
    data class Create(
        val tripId: Long,
        val name: String,
        val day: Int,
        val order: Int,
    )

    data class Update(
        val tripId: Long,
        val categoryId: Long,
        val name: String? = null,
        val day: Int? = null,
        val order: Int? = null,
        val memo: String? = null,
    )

    data class OrderUpdate(
        val tripId: Long,
        val items: List<OrderItem>,
    )

    data class OrderItem(
        val categoryId: Long,
        val order: Int,
    )
}

package com.tribe.api.itinerary.category

import com.tribe.application.itinerary.category.CategoryResult
import java.time.LocalDateTime

object CategoryResponses {
    data class CategoryResponse(
        val categoryId: Long,
        val name: String,
        val day: Int,
        val order: Int,
        val tripId: Long,
        val memo: String?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(view: CategoryResult.CategoryView) = CategoryResponse(
                categoryId = view.categoryId,
                name = view.name,
                day = view.day,
                order = view.order,
                tripId = view.tripId,
                memo = view.memo,
                createdAt = view.createdAt,
                updatedAt = view.updatedAt,
            )
        }
    }
}

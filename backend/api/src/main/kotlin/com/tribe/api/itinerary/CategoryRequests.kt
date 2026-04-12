package com.tribe.api.itinerary

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

object CategoryRequests {
    data class CreateRequest(
        @field:NotBlank(message = "카테고리 이름은 비워둘 수 없습니다.")
        val name: String,
        val day: Int,
        val order: Int,
    )

    data class UpdateRequest(
        val name: String? = null,
        val day: Int? = null,
        val order: Int? = null,
        val memo: String? = null,
    )

    data class OrderUpdateRequest(
        @field:Valid
        @field:NotNull
        val items: List<OrderItem>,
    )

    data class OrderItem(
        @field:NotNull
        val categoryId: Long,
        @field:NotNull
        val order: Int,
    )
}

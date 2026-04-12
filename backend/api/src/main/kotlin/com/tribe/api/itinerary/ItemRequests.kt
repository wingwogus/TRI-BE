package com.tribe.api.itinerary

import java.time.LocalDateTime

object ItemRequests {
    data class CreateRequest(
        val categoryId: Long,
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
}

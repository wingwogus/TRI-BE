package com.tribe.application.itinerary

import java.time.LocalDateTime

object ItemCommand {
    data class Create(
        val tripId: Long,
        val categoryId: Long,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )

    data class Update(
        val tripId: Long,
        val itemId: Long,
        val categoryId: Long? = null,
        val title: String? = null,
        val time: LocalDateTime? = null,
        val memo: String? = null,
    )
}

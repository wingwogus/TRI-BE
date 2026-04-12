package com.tribe.application.trip

object TripReviewCommand {
    data class Create(
        val tripId: Long,
        val concept: String? = null,
    )
}

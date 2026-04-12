package com.tribe.api.trip

object TripReviewRequests {
    data class CreateReviewRequest(
        val concept: String? = null,
    )
}

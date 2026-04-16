package com.tribe.api.trip.review

import com.tribe.application.trip.review.TripReviewCommand

object TripReviewRequests {
    data class CreateReviewRequest(
        val concept: String? = null,
    ) {
        fun toCommand(tripId: Long): TripReviewCommand.Create = TripReviewCommand.Create(
            tripId = tripId,
            concept = concept,
        )
    }
}

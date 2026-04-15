package com.tribe.api.trip.review

import com.tribe.application.trip.review.TripReviewResult
import java.time.LocalDateTime

object TripReviewResponses {
    data class PlaceTypeSummaryResponse(
        val primaryType: String?,
        val types: List<String>,
        val localizedPrimaryLabel: String?,
    )

    data class PhotoHintResponse(
        val name: String?,
        val photoUri: String?,
    )

    data class PlaceDetailSummaryResponse(
        val businessStatus: String?,
        val rating: Double?,
        val userRatingCount: Int?,
        val editorialSummary: String?,
    )

    data class RecommendedPlaceResponse(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummaryResponse?,
        val normalizedCategoryKey: String?,
        val photoHint: PhotoHintResponse?,
        val placeDetailSummary: PlaceDetailSummaryResponse?,
    )

    data class ReviewDetailResponse(
        val reviewId: Long,
        val concept: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
        val recommendedPlaces: List<RecommendedPlaceResponse>,
    ) {
        companion object {
            fun from(result: TripReviewResult.ReviewDetail) = ReviewDetailResponse(
                reviewId = result.reviewId,
                concept = result.concept,
                content = result.content,
                createdAt = result.createdAt,
                recommendedPlaces = result.recommendedPlaces.map {
                    RecommendedPlaceResponse(
                        placeId = it.placeId,
                        externalPlaceId = it.externalPlaceId,
                        placeName = it.placeName,
                        address = it.address,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        placeTypeSummary = it.placeTypeSummary?.let { summary ->
                            PlaceTypeSummaryResponse(summary.primaryType, summary.types, summary.localizedPrimaryLabel)
                        },
                        normalizedCategoryKey = it.normalizedCategoryKey?.name,
                        photoHint = it.photoHint?.let { hint -> PhotoHintResponse(hint.name, hint.photoUri) },
                        placeDetailSummary = it.placeDetailSummary?.let { summary ->
                            PlaceDetailSummaryResponse(summary.businessStatus, summary.rating, summary.userRatingCount, summary.editorialSummary)
                        },
                    )
                },
            )
        }
    }

    data class SimpleReviewInfoResponse(
        val reviewId: Long,
        val title: String?,
        val concept: String?,
        val createdAt: LocalDateTime?,
    ) {
        companion object {
            fun from(result: TripReviewResult.SimpleReviewInfo) = SimpleReviewInfoResponse(
                reviewId = result.reviewId,
                title = result.title,
                concept = result.concept,
                createdAt = result.createdAt,
            )
        }
    }
}

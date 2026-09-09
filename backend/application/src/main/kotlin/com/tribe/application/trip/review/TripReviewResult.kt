package com.tribe.application.trip.review

import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.itinerary.place.PlaceTypeSummary
import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.domain.trip.review.TripReview
import java.time.LocalDateTime

/**
 * 여행 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object TripReviewResult {
    data class PhotoHint(
        val name: String?,
        val photoUri: String?,
    )

    data class ReviewDetail(
        val reviewId: Long,
        val concept: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
        val recommendedPlaces: List<RecommendedPlaceResult>,
        val routeOptimization: RouteOptimizationResult?,
    ) {
        companion object {
            fun from(review: TripReview, routeOptimization: RouteOptimizationResult? = null): ReviewDetail {
                val assembler = PlaceResultAssembler()
                return ReviewDetail(
                    reviewId = review.id,
                    concept = review.concept,
                    content = review.content,
                    createdAt = review.createdAt,
                    recommendedPlaces = review.recommendedPlaces.map {
                        val placeTypeSummary = assembler.toPlaceTypeSummary(it.place)
                        RecommendedPlaceResult(
                            placeId = it.place.id,
                            externalPlaceId = it.place.externalPlaceId,
                            placeName = it.place.name,
                            address = it.place.address,
                            latitude = it.place.latitude.toDouble(),
                            longitude = it.place.longitude.toDouble(),
                            placeTypeSummary = placeTypeSummary,
                            normalizedCategoryKey = PlaceResultAssembler.toNormalizedCategoryKey(placeTypeSummary),
                            photoHint = null,
                            placeDetailSummary = assembler.toDetailSummary(it.place),
                        )
                    },
                    routeOptimization = routeOptimization,
                )
            }
        }
    }

    data class SimpleReviewInfo(
        val reviewId: Long,
        val title: String?,
        val concept: String?,
        val createdAt: LocalDateTime?,
    ) {
        companion object {
            fun from(review: TripReview): SimpleReviewInfo {
                return SimpleReviewInfo(
                    reviewId = review.id,
                    title = extractTitle(review.content),
                    concept = review.concept,
                    createdAt = review.createdAt,
                )
            }

            private fun extractTitle(text: String): String? {
                return if (text.startsWith("## ")) {
                    text.substringAfter("## ").substringBefore("\n")
                } else null
            }
        }
    }

    data class RecommendedPlaceResult(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummary?,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
    )

    data class RouteOptimizationResult(
        val days: List<RouteOptimizationDay>,
    )

    data class RouteOptimizationDay(
        val visitDay: Int,
        val currentOrder: List<RouteOptimizationItem>,
        val optimizedOrder: List<RouteOptimizationItem>,
        val currentDurationSeconds: Long?,
        val optimizedDurationSeconds: Long?,
        val savedDurationSeconds: Long?,
        val currentLegs: List<RouteOptimizationLeg>,
        val optimizedLegs: List<RouteOptimizationLeg>,
        val warnings: List<String>,
    )

    data class RouteOptimizationItem(
        val itemId: Long,
        val itemOrder: Int,
        val name: String,
        val placeId: Long?,
        val externalPlaceId: String?,
    )

    data class RouteOptimizationLeg(
        val originItemId: Long,
        val destinationItemId: Long,
        val originName: String,
        val destinationName: String,
        val durationText: String?,
        val durationSeconds: Long?,
        val distanceText: String?,
        val distanceMeters: Long?,
    )
}

package com.tribe.api.trip.review

import com.tribe.api.itinerary.place.PlaceResponses
import com.tribe.application.trip.review.TripReviewResult
import java.time.LocalDateTime

object TripReviewResponses {
    data class RecommendedPlaceResponse(
        val placeId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceResponses.PlaceTypeSummaryResponse?,
        val normalizedCategoryKey: String?,
        val photoHint: PlaceResponses.PhotoHintResponse?,
        val placeDetailSummary: PlaceResponses.PlaceDetailSummaryResponse?,
    )

    data class ReviewDetailResponse(
        val reviewId: Long,
        val concept: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
        val recommendedPlaces: List<RecommendedPlaceResponse>,
        val routeOptimization: RouteOptimizationResponse?,
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
                        placeTypeSummary = it.placeTypeSummary?.let(PlaceResponses.PlaceTypeSummaryResponse::from),
                        normalizedCategoryKey = it.normalizedCategoryKey?.name,
                        photoHint = it.photoHint?.let { hint -> PlaceResponses.PhotoHintResponse(hint.name, hint.photoUri) },
                        placeDetailSummary = it.placeDetailSummary?.let(PlaceResponses.PlaceDetailSummaryResponse::from),
                    )
                },
                routeOptimization = result.routeOptimization?.let(RouteOptimizationResponse::from),
            )
        }
    }

    data class RouteOptimizationResponse(
        val days: List<RouteOptimizationDayResponse>,
    ) {
        companion object {
            fun from(result: TripReviewResult.RouteOptimizationResult) = RouteOptimizationResponse(
                days = result.days.map(RouteOptimizationDayResponse::from),
            )
        }
    }

    data class RouteOptimizationDayResponse(
        val visitDay: Int,
        val currentOrder: List<RouteOptimizationItemResponse>,
        val optimizedOrder: List<RouteOptimizationItemResponse>,
        val currentDurationSeconds: Long?,
        val optimizedDurationSeconds: Long?,
        val savedDurationSeconds: Long?,
        val currentLegs: List<RouteOptimizationLegResponse>,
        val optimizedLegs: List<RouteOptimizationLegResponse>,
        val warnings: List<String>,
    ) {
        companion object {
            fun from(result: TripReviewResult.RouteOptimizationDay) = RouteOptimizationDayResponse(
                visitDay = result.visitDay,
                currentOrder = result.currentOrder.map(RouteOptimizationItemResponse::from),
                optimizedOrder = result.optimizedOrder.map(RouteOptimizationItemResponse::from),
                currentDurationSeconds = result.currentDurationSeconds,
                optimizedDurationSeconds = result.optimizedDurationSeconds,
                savedDurationSeconds = result.savedDurationSeconds,
                currentLegs = result.currentLegs.map(RouteOptimizationLegResponse::from),
                optimizedLegs = result.optimizedLegs.map(RouteOptimizationLegResponse::from),
                warnings = result.warnings,
            )
        }
    }

    data class RouteOptimizationItemResponse(
        val itemId: Long,
        val itemOrder: Int,
        val name: String,
        val placeId: Long?,
        val externalPlaceId: String?,
    ) {
        companion object {
            fun from(result: TripReviewResult.RouteOptimizationItem) = RouteOptimizationItemResponse(
                itemId = result.itemId,
                itemOrder = result.itemOrder,
                name = result.name,
                placeId = result.placeId,
                externalPlaceId = result.externalPlaceId,
            )
        }
    }

    data class RouteOptimizationLegResponse(
        val originItemId: Long,
        val destinationItemId: Long,
        val originName: String,
        val destinationName: String,
        val durationText: String?,
        val durationSeconds: Long?,
        val distanceText: String?,
        val distanceMeters: Long?,
    ) {
        companion object {
            fun from(result: TripReviewResult.RouteOptimizationLeg) = RouteOptimizationLegResponse(
                originItemId = result.originItemId,
                destinationItemId = result.destinationItemId,
                originName = result.originName,
                destinationName = result.destinationName,
                durationText = result.durationText,
                durationSeconds = result.durationSeconds,
                distanceText = result.distanceText,
                distanceMeters = result.distanceMeters,
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

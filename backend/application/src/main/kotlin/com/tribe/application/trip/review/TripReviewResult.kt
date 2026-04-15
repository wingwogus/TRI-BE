package com.tribe.application.trip.review

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.domain.trip.review.TripReview
import java.time.LocalDateTime

object TripReviewResult {
    private val objectMapper = jacksonObjectMapper()

    data class PlaceTypeSummary(
        val primaryType: String?,
        val types: List<String>,
        val localizedPrimaryLabel: String?,
    )

    data class PhotoHint(
        val name: String?,
        val photoUri: String?,
    )

    data class PlaceDetailSummary(
        val businessStatus: String?,
        val rating: Double?,
        val userRatingCount: Int?,
        val editorialSummary: String?,
    )

    data class ReviewDetail(
        val reviewId: Long,
        val concept: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
        val recommendedPlaces: List<RecommendedPlaceResult>,
    ) {
        companion object {
            fun from(review: TripReview): ReviewDetail {
                return ReviewDetail(
                    reviewId = review.id,
                    concept = review.concept,
                    content = review.content,
                    createdAt = review.createdAt,
                    recommendedPlaces = review.recommendedPlaces.map {
                        RecommendedPlaceResult(
                            placeId = it.place.id,
                            externalPlaceId = it.place.externalPlaceId,
                            placeName = it.place.name,
                            address = it.place.address,
                            latitude = it.place.latitude.toDouble(),
                            longitude = it.place.longitude.toDouble(),
                            placeTypeSummary = it.place.googlePrimaryType?.let { _ ->
                                PlaceTypeSummary(
                                    primaryType = it.place.googlePrimaryType,
                                    types = it.place.googleTypesJson?.let { json ->
                                        runCatching { objectMapper.readValue(json, Array<String>::class.java).toList() }.getOrDefault(emptyList())
                                    } ?: emptyList(),
                                    localizedPrimaryLabel = it.place.googlePrimaryType?.replace('_', ' '),
                                )
                            },
                            photoHint = it.place.detailSnapshot?.primaryPhotoName?.let { name ->
                                PhotoHint(name = name, photoUri = "/api/v1/places/photos?name=$name")
                            },
                            placeDetailSummary = it.place.detailSnapshot?.let { snapshot ->
                                PlaceDetailSummary(
                                    businessStatus = it.place.businessStatus,
                                    rating = snapshot.rating,
                                    userRatingCount = snapshot.userRatingCount,
                                    editorialSummary = snapshot.editorialSummary,
                                )
                            },
                        )
                    },
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
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
    )
}

package com.tribe.api.itinerary.place

import com.tribe.application.itinerary.place.PlacePhotoHint
import com.tribe.application.itinerary.place.PlaceDetailSummary
import com.tribe.application.itinerary.place.PlaceTypeSummary

object PlaceResponses {
    data class PlaceTypeSummaryResponse(
        val primaryType: String?,
        val types: List<String>,
        val displayPrimaryLabel: String?,
    ) {
        companion object {
            fun from(summary: PlaceTypeSummary) = PlaceTypeSummaryResponse(
                primaryType = summary.primaryType,
                types = summary.types,
                displayPrimaryLabel = summary.displayPrimaryLabel,
            )
        }
    }

    data class PhotoHintResponse(
        val name: String?,
        val photoUri: String? = null,
    ) {
        companion object {
            fun from(hint: PlacePhotoHint) = PhotoHintResponse(
                name = hint.name,
                photoUri = hint.photoUri,
            )
        }
    }

    data class PlaceDetailSummaryResponse(
        val businessStatus: String?,
        val rating: Double?,
        val userRatingCount: Int?,
        val editorialSummary: String?,
    ) {
        companion object {
            fun from(summary: PlaceDetailSummary) = PlaceDetailSummaryResponse(
                businessStatus = summary.businessStatus,
                rating = summary.rating,
                userRatingCount = summary.userRatingCount,
                editorialSummary = summary.editorialSummary,
            )
        }
    }

    data class SearchResponse(
        val placeId: Long? = null,
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummaryResponse? = null,
        val normalizedCategoryKey: String? = null,
        val photoHint: PhotoHintResponse? = null,
        val placeDetailSummary: PlaceDetailSummaryResponse? = null,
    )

    data class DetailResponse(
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
        val formattedPhoneNumber: String?,
        val internationalPhoneNumber: String?,
        val websiteUri: String?,
        val googleMapsUri: String?,
        val regularOpeningHoursJson: String?,
        val currentOpeningHoursJson: String?,
    )
}

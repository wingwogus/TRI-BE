package com.tribe.api.itinerary.place

import java.math.BigDecimal

object PlaceRequests {
    data class PlaceTypeSummaryResponse(
        val primaryType: String?,
        val types: List<String>,
        val localizedPrimaryLabel: String?,
    )

    data class PhotoHintResponse(
        val name: String?,
        val photoUri: String? = null,
    )

    data class PlaceDetailSummaryResponse(
        val businessStatus: String?,
        val rating: Double?,
        val userRatingCount: Int?,
        val editorialSummary: String?,
    )

    data class SearchResponse(
        val placeId: Long? = null,
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val placeTypeSummary: PlaceTypeSummaryResponse? = null,
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
        val photoHint: PhotoHintResponse?,
        val placeDetailSummary: PlaceDetailSummaryResponse?,
        val formattedPhoneNumber: String?,
        val internationalPhoneNumber: String?,
        val websiteUri: String?,
        val googleMapsUri: String?,
        val regularOpeningHoursJson: String?,
        val currentOpeningHoursJson: String?,
    )

    data class WishlistAddRequest(
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
    )

    data class WishlistDeleteRequest(
        val wishlistItemIds: List<Long>,
    )
}

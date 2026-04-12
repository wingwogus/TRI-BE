package com.tribe.api.itinerary.place

import java.math.BigDecimal

object PlaceRequests {
    data class SearchResponse(
        val placeId: Long? = null,
        val externalPlaceId: String,
        val placeName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
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

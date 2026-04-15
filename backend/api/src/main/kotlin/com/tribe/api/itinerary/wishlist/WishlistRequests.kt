package com.tribe.api.itinerary.wishlist

import java.math.BigDecimal

object WishlistRequests {
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

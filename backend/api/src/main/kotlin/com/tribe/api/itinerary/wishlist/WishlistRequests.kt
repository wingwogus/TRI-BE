package com.tribe.api.itinerary.wishlist

import com.tribe.application.itinerary.wishlist.WishlistCommand
import java.math.BigDecimal

object WishlistRequests {
    data class WishlistAddRequest(
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
    ) {
        fun toCommand(tripId: Long): WishlistCommand.Add = WishlistCommand.Add(
            tripId = tripId,
            externalPlaceId = externalPlaceId,
            placeName = placeName,
            address = address,
            latitude = latitude,
            longitude = longitude,
        )
    }

    data class WishlistDeleteRequest(
        val wishlistItemIds: List<Long>,
    ) {
        fun toCommand(tripId: Long): WishlistCommand.Delete = WishlistCommand.Delete(
            tripId = tripId,
            wishlistItemIds = wishlistItemIds,
        )
    }
}

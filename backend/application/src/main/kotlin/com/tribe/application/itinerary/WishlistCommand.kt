package com.tribe.application.itinerary

import java.math.BigDecimal

object WishlistCommand {
    data class Add(
        val tripId: Long,
        val externalPlaceId: String,
        val placeName: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
    )

    data class Delete(
        val tripId: Long,
        val wishlistItemIds: List<Long>,
    )
}

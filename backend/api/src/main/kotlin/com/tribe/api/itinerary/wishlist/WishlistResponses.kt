package com.tribe.api.itinerary.wishlist

import com.tribe.application.itinerary.wishlist.WishlistResult
import java.math.BigDecimal

object WishlistResponses {
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

    data class AdderResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
    ) {
        companion object {
            fun from(adder: WishlistResult.Adder) = AdderResponse(adder.tripMemberId, adder.memberId, adder.nickname)
        }
    }

    data class WishlistItemResponse(
        val wishlistItemId: Long,
        val placeId: Long,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val placeTypeSummary: PlaceTypeSummaryResponse?,
        val photoHint: PhotoHintResponse?,
        val placeDetailSummary: PlaceDetailSummaryResponse?,
        val adder: AdderResponse,
    ) {
        companion object {
            fun from(item: WishlistResult.Item) = WishlistItemResponse(
                item.wishlistItemId,
                item.placeId,
                item.name,
                item.address,
                item.latitude,
                item.longitude,
                item.placeTypeSummary?.let {
                    PlaceTypeSummaryResponse(it.primaryType, it.types, it.localizedPrimaryLabel)
                },
                item.photoHint?.let { PhotoHintResponse(it.name, it.photoUri) },
                item.placeDetailSummary?.let {
                    PlaceDetailSummaryResponse(it.businessStatus, it.rating, it.userRatingCount, it.editorialSummary)
                },
                AdderResponse.from(item.adder),
            )
        }
    }

    data class WishlistSearchResponse(
        val content: List<WishlistItemResponse>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val totalElements: Long,
        val isLast: Boolean,
    ) {
        companion object {
            fun from(page: WishlistResult.SearchPage) = WishlistSearchResponse(
                content = page.content.map(WishlistItemResponse::from),
                pageNumber = page.pageNumber,
                pageSize = page.pageSize,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                isLast = page.isLast,
            )
        }
    }
}

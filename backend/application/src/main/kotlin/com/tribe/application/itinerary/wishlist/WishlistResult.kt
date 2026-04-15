package com.tribe.application.itinerary.wishlist

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tribe.application.itinerary.place.PlaceCategoryNormalizer
import com.tribe.application.itinerary.place.NormalizedPlaceCategoryKey
import com.tribe.domain.itinerary.wishlist.WishlistItem
import java.math.BigDecimal

object WishlistResult {
    private val objectMapper = jacksonObjectMapper()

    private fun decodeTypes(json: String?): List<String> =
        json?.let {
            runCatching { objectMapper.readValue(it, Array<String>::class.java).toList() }.getOrDefault(emptyList())
        } ?: emptyList()

    private fun toPlaceTypeSummary(primaryType: String?, googleTypesJson: String?): PlaceTypeSummary? {
        val types = decodeTypes(googleTypesJson)
        if (primaryType == null && types.isEmpty()) {
            return null
        }

        return PlaceTypeSummary(
            primaryType = primaryType,
            types = types,
            localizedPrimaryLabel = primaryType?.replace('_', ' '),
        )
    }

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

    data class Adder(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
    )

    data class Item(
        val wishlistItemId: Long,
        val placeId: Long,
        val name: String,
        val address: String?,
        val latitude: BigDecimal,
        val longitude: BigDecimal,
        val placeTypeSummary: PlaceTypeSummary?,
        val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
        val photoHint: PhotoHint?,
        val placeDetailSummary: PlaceDetailSummary?,
        val adder: Adder,
    ) {
        companion object {
            fun from(entity: WishlistItem): Item {
                return Item(
                    wishlistItemId = entity.id,
                    placeId = entity.place.id,
                    name = entity.place.name,
                    address = entity.place.address,
                    latitude = entity.place.latitude,
                    longitude = entity.place.longitude,
                    placeTypeSummary = toPlaceTypeSummary(entity.place.googlePrimaryType, entity.place.googleTypesJson),
                    normalizedCategoryKey = PlaceCategoryNormalizer.normalize(
                        entity.place.googlePrimaryType,
                        decodeTypes(entity.place.googleTypesJson),
                    ),
                    photoHint = null,
                    placeDetailSummary = entity.place.detailSnapshot?.let {
                        PlaceDetailSummary(
                            businessStatus = entity.place.businessStatus,
                            rating = it.rating,
                            userRatingCount = it.userRatingCount,
                            editorialSummary = it.editorialSummary,
                        )
                    },
                    adder = Adder(
                        tripMemberId = entity.adder.id,
                        memberId = entity.adder.member?.id,
                        nickname = entity.adder.name,
                    ),
                )
            }
        }
    }

    data class SearchPage(
        val content: List<Item>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val totalElements: Long,
        val isLast: Boolean,
    )
}

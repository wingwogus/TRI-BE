package com.tribe.application.itinerary

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.TripAuthorizationPolicy
import com.tribe.domain.itinerary.Place
import com.tribe.domain.itinerary.PlaceRepository
import com.tribe.domain.itinerary.WishlistItem
import com.tribe.domain.itinerary.WishlistItemRepository
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.TripMemberRepository
import com.tribe.domain.trip.TripRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WishlistService(
    private val wishlistItemRepository: WishlistItemRepository,
    private val placeRepository: PlaceRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val tripRepository: TripRepository,
    private val memberRepository: MemberRepository,
    private val currentActor: CurrentActor,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    fun addWishList(command: WishlistCommand.Add): WishlistResult.Item {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val memberId = currentActor.requireUserId()
        val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val trip = tripRepository.findById(command.tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val tripMember = tripMemberRepository.findByTripAndMember(trip, member)
            ?: throw BusinessException(ErrorCode.NOT_A_TRIP_MEMBER)

        if (wishlistItemRepository.existsByTrip_IdAndPlace_ExternalPlaceId(command.tripId, command.externalPlaceId)) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS)
        }

        val place = placeRepository.findByExternalPlaceId(command.externalPlaceId)
            ?: placeRepository.save(
                Place(
                    externalPlaceId = command.externalPlaceId,
                    name = command.placeName,
                    address = command.address,
                    latitude = command.latitude,
                    longitude = command.longitude,
                )
            )

        val saved = wishlistItemRepository.save(
            WishlistItem(
                trip = trip,
                place = place,
                adder = tripMember,
            )
        )
        return WishlistResult.Item.from(saved)
    }

    @Transactional(readOnly = true)
    fun searchWishList(tripId: Long, query: String, pageable: Pageable): WishlistResult.SearchPage {
        tripAuthorizationPolicy.isTripMember(tripId)
        val page = wishlistItemRepository.findAllByTrip_IdAndPlace_NameContainingIgnoreCase(tripId, query, pageable)
        return WishlistResult.SearchPage(
            content = page.content.map(WishlistResult.Item::from),
            pageNumber = page.number,
            pageSize = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            isLast = page.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun getWishList(tripId: Long, pageable: Pageable): WishlistResult.SearchPage {
        tripAuthorizationPolicy.isTripMember(tripId)
        val page = wishlistItemRepository.findAllByTrip_Id(tripId, pageable)
        return WishlistResult.SearchPage(
            content = page.content.map(WishlistResult.Item::from),
            pageNumber = page.number,
            pageSize = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            isLast = page.isLast,
        )
    }

    fun deleteWishlistItems(command: WishlistCommand.Delete) {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val ids = command.wishlistItemIds.distinct()
        if (ids.isEmpty()) return
        val existingIds = wishlistItemRepository.findIdsByTripIdAndIdIn(command.tripId, ids)
        val missing = ids.filterNot { it in existingIds }
        if (missing.isNotEmpty()) throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)
        wishlistItemRepository.deleteAllByIdInBatch(existingIds)
    }
}

package com.tribe.application.itinerary.place

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceSearchService(
    private val placeSearchGateway: PlaceSearchGateway,
) {
    fun search(query: String?, language: String, region: String?): List<PlaceSearchResult> {
        return placeSearchGateway.search(query, language, region)
    }

    fun directions(originPlaceId: String, destinationPlaceId: String, mode: String): RouteDetails? {
        val normalized = mode.trim().uppercase()
        if (normalized !in setOf("WALKING", "DRIVING", "TRANSIT")) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = mapOf("travelMode" to mode))
        }
        return placeSearchGateway.directions(originPlaceId, destinationPlaceId, normalized)
    }
}

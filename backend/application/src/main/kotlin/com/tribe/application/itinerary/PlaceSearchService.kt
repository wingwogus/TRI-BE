package com.tribe.application.itinerary

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
}

package com.tribe.application.itinerary.place

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["tribe.itinerary.place-search.enabled"], havingValue = "false")
class NoOpPlaceSearchGateway : PlaceSearchGateway {
    override fun search(query: String?, language: String, region: String?): List<PlaceSearchResult> = emptyList()
    override fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails? = null
}

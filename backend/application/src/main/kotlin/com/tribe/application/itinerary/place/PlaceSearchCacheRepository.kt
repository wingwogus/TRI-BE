package com.tribe.application.itinerary.place

import java.time.Duration

interface PlaceSearchCacheRepository {
    fun get(key: String): List<PlaceSearchGateway.SearchHit>?
    fun put(key: String, value: List<PlaceSearchGateway.SearchHit>, ttl: Duration)
}

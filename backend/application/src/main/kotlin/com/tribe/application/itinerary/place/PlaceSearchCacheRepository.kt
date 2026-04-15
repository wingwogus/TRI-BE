package com.tribe.application.itinerary.place

import java.time.Duration

interface PlaceSearchCacheRepository {
    fun get(key: String): List<PlaceSearchResult>?
    fun put(key: String, value: List<PlaceSearchResult>, ttl: Duration)
}

package com.tribe.application.itinerary

interface PlaceSearchGateway {
    fun search(query: String?, language: String, region: String?): List<PlaceSearchResult>
}

data class PlaceSearchResult(
    val externalPlaceId: String,
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

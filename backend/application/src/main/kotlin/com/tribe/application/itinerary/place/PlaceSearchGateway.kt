package com.tribe.application.itinerary.place

interface PlaceSearchGateway {
    fun search(query: String?, language: String, region: String?): List<PlaceSearchResult>
    fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails?
}

data class PlaceSearchResult(
    val externalPlaceId: String,
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

data class RouteDetails(
    val travelMode: String,
    val originPlace: PlaceSearchResult,
    val destinationPlace: PlaceSearchResult,
    val totalDuration: String,
    val totalDistance: String,
    val steps: List<RouteStep>,
) {
    data class RouteStep(
        val travelMode: String,
        val instructions: String,
        val duration: String,
        val distance: String,
        val transitDetails: TransitDetails?,
    )

    data class TransitDetails(
        val lineName: String,
        val vehicleType: String,
        val vehicleIconUrl: String?,
        val numStops: Int,
        val departureStop: String,
        val arrivalStop: String,
    )
}

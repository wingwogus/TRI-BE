package com.tribe.api.itinerary.place

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchGateway
import com.tribe.application.itinerary.place.PlaceSearchResult
import com.tribe.application.itinerary.place.RouteDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
@ConditionalOnProperty(name = ["tribe.itinerary.place-search.enabled"], havingValue = "true", matchIfMissing = true)
class GooglePlaceSearchGateway(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${google.maps.key}") private val apiKey: String,
) : PlaceSearchGateway {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient = webClientBuilder.build()

    override fun search(query: String?, language: String, region: String?): List<PlaceSearchResult> {
        val response = webClient.post()
            .uri("https://places.googleapis.com/v1/places:searchText")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location")
            .bodyValue(
                mapOf(
                    "textQuery" to query,
                    "languageCode" to language,
                    "regionCode" to region,
                )
            )
            .retrieve()
            .bodyToMono(PlacesResponse::class.java)
            .doOnError { logger.error("Error calling Google Places API", it) }
            .block()
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

        return response.places?.map {
            PlaceSearchResult(
                externalPlaceId = it.id,
                placeName = it.displayName?.text ?: "이름 없음",
                address = it.formattedAddress ?: "주소 정보 없음",
                latitude = it.location?.latitude ?: 0.0,
                longitude = it.location?.longitude ?: 0.0,
            )
        } ?: emptyList()
    }

    override fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails? {
        val response = webClient.get()
            .uri { builder ->
                builder
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/directions/json")
                    .queryParam("origin", "place_id:$originPlaceId")
                    .queryParam("destination", "place_id:$destinationPlaceId")
                    .queryParam("language", "ko")
                    .queryParam("mode", travelMode.lowercase())
                    .queryParam("key", apiKey)
                    .build()
            }
            .retrieve()
            .bodyToMono(DirectionsRawResponse::class.java)
            .doOnError { logger.error("Error calling Google Directions API", it) }
            .block()
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

        if (response.status != "OK") {
            return null
        }

        val route = response.routes.firstOrNull() ?: return null
        val leg = route.legs.firstOrNull() ?: return null
        val origin = search(route.originName ?: "", "ko", null).firstOrNull()
            ?: PlaceSearchResult(originPlaceId, route.originName ?: "출발지", route.originAddress ?: "", 0.0, 0.0)
        val destination = search(route.destinationName ?: "", "ko", null).firstOrNull()
            ?: PlaceSearchResult(destinationPlaceId, route.destinationName ?: "도착지", route.destinationAddress ?: "", 0.0, 0.0)

        return RouteDetails(
            travelMode = travelMode,
            originPlace = origin,
            destinationPlace = destination,
            totalDuration = leg.duration?.text ?: "",
            totalDistance = leg.distance?.text ?: "",
            steps = leg.steps.map { rawStep ->
                RouteDetails.RouteStep(
                    travelMode = rawStep.travelMode ?: "",
                    instructions = rawStep.htmlInstructions?.replace(Regex("<[^>]*>"), "") ?: "",
                    duration = rawStep.duration?.text ?: "",
                    distance = rawStep.distance?.text ?: "",
                    transitDetails = rawStep.transitDetails?.let { transit ->
                        RouteDetails.TransitDetails(
                            lineName = transit.line?.shortName ?: "이름 없음",
                            vehicleType = transit.line?.vehicle?.type ?: "",
                            vehicleIconUrl = transit.line?.vehicle?.icon,
                            numStops = transit.numStops ?: 0,
                            departureStop = transit.departureStop?.name ?: "",
                            arrivalStop = transit.arrivalStop?.name ?: "",
                        )
                    },
                )
            },
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PlacesResponse(
        val places: List<PlaceResult>?,
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class PlaceResult(
            val id: String,
            val formattedAddress: String?,
            val location: Location?,
            val displayName: DisplayName?,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Location(
            val latitude: Double,
            val longitude: Double,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class DisplayName(
            val text: String,
            val languageCode: String?,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DirectionsRawResponse(
        val status: String,
        val routes: List<Route> = emptyList(),
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Route(
            val legs: List<Leg> = emptyList(),
            val summary: String? = null,
            val copyrights: String? = null,
            val warnings: List<String> = emptyList(),
            val waypointOrder: List<Int> = emptyList(),
            val overviewPolyline: Polyline? = null,
            val bounds: Map<String, Any>? = null,
            val fare: Map<String, Any>? = null,
            val originAddress: String? = null,
            val destinationAddress: String? = null,
            val originName: String? = null,
            val destinationName: String? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Polyline(val points: String? = null)

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Leg(
            val distance: TextValue? = null,
            val duration: TextValue? = null,
            val steps: List<Step> = emptyList(),
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Step(
            val travelMode: String? = null,
            val htmlInstructions: String? = null,
            val distance: TextValue? = null,
            val duration: TextValue? = null,
            val transitDetails: TransitDetails? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class TextValue(
            val text: String? = null,
            val value: Long? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class TransitDetails(
            val departureStop: Stop? = null,
            val arrivalStop: Stop? = null,
            val numStops: Int? = null,
            val line: Line? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Stop(
            val name: String? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Line(
            val shortName: String? = null,
            val vehicle: Vehicle? = null,
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Vehicle(
            val type: String? = null,
            val icon: String? = null,
        )
    }
}

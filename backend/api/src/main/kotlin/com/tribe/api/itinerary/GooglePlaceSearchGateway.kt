package com.tribe.api.itinerary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.PlaceSearchGateway
import com.tribe.application.itinerary.PlaceSearchResult
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
}

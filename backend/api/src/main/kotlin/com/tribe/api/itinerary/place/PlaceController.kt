package com.tribe.api.itinerary.place

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.place.PlaceSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeSearchService: PlaceSearchService,
) {
    @GetMapping("/search")
    fun searchPlaces(
        @RequestParam query: String?,
        @RequestParam region: String?,
        @RequestParam(defaultValue = "ko") language: String,
    ): ResponseEntity<ApiResponse<List<PlaceRequests.SearchResponse>>> {
        val result = placeSearchService.search(query, language, region)
            .map {
                PlaceRequests.SearchResponse(
                    externalPlaceId = it.externalPlaceId,
                    placeName = it.placeName,
                    address = it.address,
                    latitude = it.latitude,
                    longitude = it.longitude,
                )
            }
        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}

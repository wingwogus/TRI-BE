package com.tribe.api.itinerary.place

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.place.PlaceSearchService
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

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
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false) radiusMeters: Int?,
        @RequestParam(required = false) regionContextKey: String?,
    ): ResponseEntity<ApiResponse<List<PlaceResponses.SearchResponse>>> {
        val result = placeSearchService.search(query, language, region, latitude, longitude, radiusMeters, regionContextKey)
            .map {
                PlaceResponses.SearchResponse(
                    placeId = it.placeId,
                    externalPlaceId = it.externalPlaceId,
                    placeName = it.placeName,
                    address = it.address,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    placeTypeSummary = it.placeTypeSummary?.let(PlaceResponses.PlaceTypeSummaryResponse::from),
                    normalizedCategoryKey = it.normalizedCategoryKey?.name,
                    photoHint = it.photoHint?.let(PlaceResponses.PhotoHintResponse::from),
                    placeDetailSummary = it.placeDetailSummary?.let(PlaceResponses.PlaceDetailSummaryResponse::from),
                )
            }
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping("/{placeId}")
    fun getPlaceDetail(
        @PathVariable placeId: Long,
        @RequestParam(defaultValue = "ko") language: String,
    ): ResponseEntity<ApiResponse<PlaceResponses.DetailResponse>> {
        val detail = placeSearchService.getPlaceDetail(placeId, language)
        return ResponseEntity.ok(
            ApiResponse.ok(
                PlaceResponses.DetailResponse(
                    placeId = detail.placeId,
                    externalPlaceId = detail.externalPlaceId,
                    placeName = detail.placeName,
                    address = detail.address,
                    latitude = detail.latitude,
                    longitude = detail.longitude,
                    placeTypeSummary = detail.placeTypeSummary?.let(PlaceResponses.PlaceTypeSummaryResponse::from),
                    normalizedCategoryKey = detail.normalizedCategoryKey?.name,
                    photoHint = detail.photoHint?.let(PlaceResponses.PhotoHintResponse::from),
                    placeDetailSummary = detail.placeDetailSummary?.let(PlaceResponses.PlaceDetailSummaryResponse::from),
                    formattedPhoneNumber = detail.formattedPhoneNumber,
                    internationalPhoneNumber = detail.internationalPhoneNumber,
                    websiteUri = detail.websiteUri,
                    googleMapsUri = detail.googleMapsUri,
                    regularOpeningHoursJson = detail.regularOpeningHoursJson,
                    currentOpeningHoursJson = detail.currentOpeningHoursJson,
                ),
            ),
        )
    }

    @GetMapping("/photos")
    fun getPlacePhoto(
        @RequestParam name: String,
        @RequestParam(defaultValue = "320") maxWidthPx: Int,
    ): ResponseEntity<*> {
        val media = placeSearchService.getPhoto(name, maxWidthPx)
        media.redirectUri?.let { redirectUri ->
            return ResponseEntity.status(302)
                .location(URI.create(redirectUri))
                .build<Any>()
        }
        return ResponseEntity.ok()
            .contentType(media.contentType?.let(MediaType::parseMediaType) ?: MediaType.IMAGE_JPEG)
            .body(ByteArrayResource(media.bytes ?: ByteArray(0)))
    }
}

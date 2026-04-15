package com.tribe.application.itinerary.place

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.domain.itinerary.place.Place
import org.springframework.stereotype.Component

data class PlaceDetailSummary(
    val businessStatus: String?,
    val rating: Double?,
    val userRatingCount: Int?,
    val editorialSummary: String?,
)

data class PlaceDetailView(
    val placeId: Long,
    val externalPlaceId: String,
    val placeName: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val placeTypeSummary: PlaceTypeSummary?,
    val photoHint: PlacePhotoHint?,
    val placeDetailSummary: PlaceDetailSummary?,
    val formattedPhoneNumber: String?,
    val internationalPhoneNumber: String?,
    val websiteUri: String?,
    val googleMapsUri: String?,
    val regularOpeningHoursJson: String?,
    val currentOpeningHoursJson: String?,
)

@Component
class PlaceViewAssembler(
    private val objectMapper: ObjectMapper,
) {
    fun toPlaceTypeSummary(place: Place?): PlaceTypeSummary? {
        if (place == null) return null
        val types = place.googleTypesJson?.let { payload ->
            runCatching {
                objectMapper.readValue(payload, object : TypeReference<List<String>>() {})
            }.getOrDefault(emptyList())
        } ?: emptyList()

        return if (place.googlePrimaryType == null && types.isEmpty()) {
            null
        } else {
            PlaceTypeSummary(
                primaryType = place.googlePrimaryType,
                types = types,
                localizedPrimaryLabel = place.googlePrimaryType?.replace('_', ' '),
            )
        }
    }

    fun toPhotoHint(place: Place?): PlacePhotoHint? =
        place?.detailSnapshot?.primaryPhotoName?.let {
            PlacePhotoHint(
                name = it,
                photoUri = "/api/v1/places/photos?name=$it",
            )
        }

    fun toDetailSummary(place: Place?): PlaceDetailSummary? {
        val snapshot = place?.detailSnapshot ?: return null
        return PlaceDetailSummary(
            businessStatus = place.businessStatus,
            rating = snapshot.rating,
            userRatingCount = snapshot.userRatingCount,
            editorialSummary = snapshot.editorialSummary,
        )
    }

    fun toDetailView(place: Place): PlaceDetailView = PlaceDetailView(
        placeId = place.id,
        externalPlaceId = place.externalPlaceId,
        placeName = place.name,
        address = place.address,
        latitude = place.latitude.toDouble(),
        longitude = place.longitude.toDouble(),
        placeTypeSummary = toPlaceTypeSummary(place),
        photoHint = toPhotoHint(place),
        placeDetailSummary = toDetailSummary(place),
        formattedPhoneNumber = place.detailSnapshot?.formattedPhoneNumber,
        internationalPhoneNumber = place.detailSnapshot?.internationalPhoneNumber,
        websiteUri = place.detailSnapshot?.websiteUri,
        googleMapsUri = place.detailSnapshot?.googleMapsUri,
        regularOpeningHoursJson = place.detailSnapshot?.regularOpeningHoursJson,
        currentOpeningHoursJson = place.detailSnapshot?.currentOpeningHoursJson,
    )
}

package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.place.Place
import org.springframework.stereotype.Component

data class PlaceDetailView(
    val placeId: Long,
    val externalPlaceId: String,
    val placeName: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val placeTypeSummary: PlaceTypeSummary?,
    val normalizedCategoryKey: NormalizedPlaceCategoryKey?,
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
class PlaceViewAssembler {
    fun toNormalizedCategoryKey(place: Place?): NormalizedPlaceCategoryKey? =
        toPlaceTypeSummary(place)?.let { summary ->
            PlaceCategoryNormalizer.normalize(summary.primaryType, summary.types)
        }

    fun toPlaceTypeSummary(place: Place?): PlaceTypeSummary? {
        if (place == null) return null
        val types = PlaceTypeSummaryFactory.decodeGoogleTypes(place.googleTypesJson)
        return PlaceTypeSummaryFactory.fromRawTypes(place.googlePrimaryType, types)
    }

    fun toPhotoHint(place: Place?): PlacePhotoHint? = null

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
        normalizedCategoryKey = toNormalizedCategoryKey(place),
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

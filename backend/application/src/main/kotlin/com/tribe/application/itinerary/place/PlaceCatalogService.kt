package com.tribe.application.itinerary.place

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceDetailSnapshot
import com.tribe.domain.itinerary.place.PlaceDetailSnapshotRepository
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriod
import com.tribe.domain.itinerary.place.PlaceRegularOpeningPeriodRepository
import com.tribe.domain.itinerary.place.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class PlaceCatalogService(
    private val objectMapper: ObjectMapper,
    private val placeViewAssembler: PlaceViewAssembler,
    private val placeRepository: PlaceRepository,
    private val detailSnapshotRepository: PlaceDetailSnapshotRepository,
    private val openingPeriodRepository: PlaceRegularOpeningPeriodRepository,
    private val placeSearchGateway: PlaceSearchGateway,
) {
    fun findExistingPlaces(results: List<PlaceSearchResult>): Map<String, Place> {
        if (results.isEmpty()) return emptyMap()
        return placeRepository.findByExternalPlaceIdIn(results.map { it.externalPlaceId }).associateBy { it.externalPlaceId }
    }

    fun getOrCreateAndEnrich(
        externalPlaceId: String,
        placeName: String,
        address: String?,
        latitude: BigDecimal,
        longitude: BigDecimal,
        language: String = "ko",
    ): Place {
        val place = placeRepository.findByExternalPlaceId(externalPlaceId) ?: placeRepository.save(
            Place(
                externalPlaceId = externalPlaceId,
                name = placeName,
                address = address,
                latitude = latitude,
                longitude = longitude,
            ),
        )

        enrichDetailsIfNeeded(place, language)

        return place
    }

    fun mergeWithCanonical(results: List<PlaceSearchResult>): List<PlaceSearchResult> {
        val existingMap = findExistingPlaces(results)
        return results.map { result ->
            val place = existingMap[result.externalPlaceId]
            result.copy(
                placeId = place?.id,
                placeTypeSummary = result.placeTypeSummary ?: placeViewAssembler.toPlaceTypeSummary(place),
                photoHint = result.photoHint ?: placeViewAssembler.toPhotoHint(place),
                placeDetailSummary = result.placeDetailSummary ?: placeViewAssembler.toDetailSummary(place),
            )
        }
    }

    fun enrichDetailsIfNeeded(place: Place, language: String = "ko"): Place {
        if (place.detailsSyncedAt != null) return place
        val details = placeSearchGateway.getPlaceDetails(place.externalPlaceId, language) ?: return place
        applyDetails(place, details)
        return place
    }

    private fun applyDetails(place: Place, details: PlaceDetailsResult) {
        place.googlePrimaryType = details.placeTypeSummary?.primaryType
        place.googleTypesJson = details.placeTypeSummary?.types?.let(objectMapper::writeValueAsString)
        place.businessStatus = details.businessStatus
        place.utcOffsetMinutes = details.utcOffsetMinutes
        place.typeSummarySyncedAt = LocalDateTime.now()
        place.detailsSyncedAt = LocalDateTime.now()

        val snapshot = detailSnapshotRepository.findById(place.id).orElse(
            PlaceDetailSnapshot(place = place)
        )
        snapshot.formattedPhoneNumber = details.formattedPhoneNumber
        snapshot.internationalPhoneNumber = details.internationalPhoneNumber
        snapshot.websiteUri = details.websiteUri
        snapshot.googleMapsUri = details.googleMapsUri
        snapshot.rating = details.rating
        snapshot.userRatingCount = details.userRatingCount
        snapshot.priceLevel = details.priceLevel
        snapshot.regularOpeningHoursJson = details.regularOpeningHoursJson
        snapshot.currentOpeningHoursJson = details.currentOpeningHoursJson
        snapshot.primaryPhotoName = details.primaryPhotoName
        snapshot.editorialSummary = details.editorialSummary
        snapshot.detailsSyncedAt = place.detailsSyncedAt
        snapshot.updatedAt = LocalDateTime.now()
        place.detailSnapshot = detailSnapshotRepository.save(snapshot)

        openingPeriodRepository.deleteAllByPlaceId(place.id)
        val periods = details.regularOpeningPeriods.map {
            PlaceRegularOpeningPeriod(
                place = place,
                dayOfWeek = it.dayOfWeek,
                openMinute = it.openMinute,
                closeMinute = it.closeMinute,
                isOvernight = it.isOvernight,
                sequenceNo = it.sequenceNo,
            )
        }
        place.regularOpeningPeriods.clear()
        place.regularOpeningPeriods.addAll(periods)
        openingPeriodRepository.saveAll(periods)
    }
}

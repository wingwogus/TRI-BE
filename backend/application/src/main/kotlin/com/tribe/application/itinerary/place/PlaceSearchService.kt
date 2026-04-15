package com.tribe.application.itinerary.place

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.itinerary.place.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.Locale

@Service
@Transactional(readOnly = true)
class PlaceSearchService(
    private val placeSearchGateway: PlaceSearchGateway,
    private val placeSearchCacheRepository: PlaceSearchCacheRepository,
    private val placeCatalogService: PlaceCatalogService,
    private val placeRepository: PlaceRepository,
    private val placeViewAssembler: PlaceViewAssembler,
) {
    companion object {
        private const val MAX_RADIUS_METERS = 50_000
    }

    fun search(
        query: String?,
        language: String,
        region: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Int? = null,
        regionContextKey: String? = null,
    ): List<PlaceSearchResult> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val normalizedRegion = region
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }
        val normalizedRadius = if (latitude != null && longitude != null) {
            (radiusMeters ?: MAX_RADIUS_METERS).coerceIn(1, MAX_RADIUS_METERS)
        } else {
            null
        }
        val context = PlaceSearchContext(
            regionCode = normalizedRegion,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = normalizedRadius,
            regionContextKey = regionContextKey,
        )
        val cacheKey = listOf(
            normalizedQuery.lowercase(),
            language.lowercase(),
            context.regionContextKey ?: normalizedRegion.orEmpty(),
            latitude?.toString().orEmpty(),
            longitude?.toString().orEmpty(),
            normalizedRadius?.toString().orEmpty(),
        ).joinToString("|")

        val cached = placeSearchCacheRepository.get(cacheKey)
        if (cached != null) {
            return placeCatalogService.mergeWithCanonical(cached)
        }

        val results = placeSearchGateway.search(normalizedQuery, language, context)
        placeSearchCacheRepository.put(cacheKey, results, Duration.ofHours(6))
        return placeCatalogService.mergeWithCanonical(results)
    }

    fun directions(originPlaceId: String, destinationPlaceId: String, mode: String): RouteDetails? {
        val normalized = mode.trim().uppercase()
        if (normalized !in setOf("WALKING", "DRIVING", "TRANSIT")) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = mapOf("travelMode" to mode))
        }
        return placeSearchGateway.directions(originPlaceId, destinationPlaceId, normalized)
    }

    fun getPhoto(name: String, maxWidthPx: Int): PlacePhotoMedia =
        placeSearchGateway.getPhoto(name, maxWidthPx)
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)

    @Transactional
    fun getPlaceDetail(placeId: Long, language: String = "ko"): PlaceDetailView {
        val place = placeRepository.findById(placeId)
            .orElseThrow { BusinessException(ErrorCode.PLACE_NOT_FOUND) }
        placeCatalogService.enrichDetailsIfNeeded(place, language)
        return placeViewAssembler.toDetailView(place)
    }
}

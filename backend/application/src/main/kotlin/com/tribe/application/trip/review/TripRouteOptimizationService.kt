package com.tribe.application.trip.review

import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.trip.core.Trip
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Service
@Transactional(readOnly = true)
class TripRouteOptimizationService(
    private val placeSearchService: PlaceSearchService,
) {
    fun preview(trip: Trip, mode: String = DEFAULT_MODE): TripReviewResult.RouteOptimizationResult {
        val days = trip.itineraryItems
            .groupBy { it.visitDay }
            .toSortedMap()
            .map { (visitDay, items) ->
                optimizeDay(visitDay, items.sortedBy { it.order }, mode)
            }

        return TripReviewResult.RouteOptimizationResult(days)
    }

    private fun optimizeDay(
        visitDay: Int,
        items: List<ItineraryItem>,
        mode: String,
    ): TripReviewResult.RouteOptimizationDay {
        val warnings = mutableListOf<String>()
        val placeItems = items.filter { it.place != null }
        val routeCache = mutableMapOf<Pair<String, String>, RouteDetails?>()
        if (items.any { it.place == null }) {
            warnings += "장소 정보가 없는 일정은 동선 최적화 계산에서 고정됩니다."
        }
        if (items.any { it.time != null }) {
            warnings += "시간이 지정된 일정이 있어 적용 후 시간 조정이 필요할 수 있습니다."
        }
        if (placeItems.size > MAX_OPTIMIZABLE_ITEMS_PER_DAY) {
            warnings += "하루 일정이 많아 현재 순서를 유지합니다."
        }

        val currentOrder = items.map { item -> item.toRouteItem(item.order) }
        if (placeItems.size < 2 || placeItems.size > MAX_OPTIMIZABLE_ITEMS_PER_DAY) {
            val currentLegs = buildLegs(items, mode, routeCache, warnings)
            val currentDuration = sumDuration(currentLegs)
            return TripReviewResult.RouteOptimizationDay(
                visitDay = visitDay,
                currentOrder = currentOrder,
                optimizedOrder = currentOrder,
                currentDurationSeconds = currentDuration,
                optimizedDurationSeconds = currentDuration,
                savedDurationSeconds = 0,
                currentLegs = currentLegs,
                optimizedLegs = currentLegs,
                warnings = warnings.distinct(),
            )
        }

        val optimizedPlaceItems = nearestNeighbor(placeItems, mode, routeCache, warnings)
        val optimizedItems = mergeOptimizedPlaceItems(items, optimizedPlaceItems)
        val currentLegs = buildLegs(items, mode, routeCache, warnings)
        val optimizedLegs = buildLegs(optimizedItems, mode, routeCache, warnings)
        val currentDuration = sumDuration(currentLegs)
        val optimizedDuration = sumDuration(optimizedLegs)

        return TripReviewResult.RouteOptimizationDay(
            visitDay = visitDay,
            currentOrder = currentOrder,
            optimizedOrder = optimizedItems.mapIndexed { index, item -> item.toRouteItem(index + 1) },
            currentDurationSeconds = currentDuration,
            optimizedDurationSeconds = optimizedDuration,
            savedDurationSeconds = if (currentDuration != null && optimizedDuration != null) {
                (currentDuration - optimizedDuration).coerceAtLeast(0)
            } else {
                null
            },
            currentLegs = currentLegs,
            optimizedLegs = optimizedLegs,
            warnings = warnings.distinct(),
        )
    }

    private fun nearestNeighbor(
        placeItems: List<ItineraryItem>,
        mode: String,
        routeCache: MutableMap<Pair<String, String>, RouteDetails?>,
        warnings: MutableList<String>,
    ): List<ItineraryItem> {
        val result = mutableListOf(placeItems.first())
        val remaining = placeItems.drop(1).toMutableList()
        while (remaining.isNotEmpty()) {
            val current = result.last()
            val next = remaining.minWithOrNull(compareBy<ItineraryItem> {
                routeBetween(current, it, mode, routeCache, warnings)?.totalDurationSeconds ?: Long.MAX_VALUE
            }.thenBy {
                distanceBetween(current, it)
            }) ?: remaining.first()
            result += next
            remaining.remove(next)
        }
        return result
    }

    private fun mergeOptimizedPlaceItems(
        originalItems: List<ItineraryItem>,
        optimizedPlaceItems: List<ItineraryItem>,
    ): List<ItineraryItem> {
        val iterator = optimizedPlaceItems.iterator()
        return originalItems.map { item ->
            if (item.place == null) item else iterator.next()
        }
    }

    private fun buildLegs(
        items: List<ItineraryItem>,
        mode: String,
        routeCache: MutableMap<Pair<String, String>, RouteDetails?>,
        warnings: MutableList<String>,
    ): List<TripReviewResult.RouteOptimizationLeg> =
        items.zipWithNext().mapNotNull { (origin, destination) ->
            if (origin.place == null || destination.place == null) return@mapNotNull null
            val route = routeBetween(origin, destination, mode, routeCache, warnings)
            TripReviewResult.RouteOptimizationLeg(
                originItemId = origin.id,
                destinationItemId = destination.id,
                originName = origin.displayName(),
                destinationName = destination.displayName(),
                durationText = route?.totalDuration,
                durationSeconds = route?.totalDurationSeconds,
                distanceText = route?.totalDistance,
                distanceMeters = route?.totalDistanceMeters,
            )
        }

    private fun routeBetween(
        origin: ItineraryItem,
        destination: ItineraryItem,
        mode: String,
        routeCache: MutableMap<Pair<String, String>, RouteDetails?>,
        warnings: MutableList<String>,
    ): RouteDetails? {
        val originPlace = origin.place ?: return null
        val destinationPlace = destination.place ?: return null
        val key = originPlace.externalPlaceId to destinationPlace.externalPlaceId
        if (routeCache.containsKey(key)) {
            return routeCache[key]
        }
        val route = placeSearchService.directions(originPlace.externalPlaceId, destinationPlace.externalPlaceId, mode)
        routeCache[key] = route
        if (route == null) {
            warnings += "${origin.displayName()} -> ${destination.displayName()} 이동 정보를 찾지 못했습니다."
        }
        return route
    }

    private fun sumDuration(legs: List<TripReviewResult.RouteOptimizationLeg>): Long? {
        if (legs.isEmpty()) return 0
        val durations = legs.map { it.durationSeconds }
        if (durations.any { it == null }) return null
        return durations.filterNotNull().sum()
    }

    private fun distanceBetween(origin: ItineraryItem, destination: ItineraryItem): Double {
        val originPlace = origin.place ?: return Double.MAX_VALUE
        val destinationPlace = destination.place ?: return Double.MAX_VALUE
        val lat1 = Math.toRadians(originPlace.latitude.toDouble())
        val lat2 = Math.toRadians(destinationPlace.latitude.toDouble())
        val deltaLat = lat2 - lat1
        val deltaLng = Math.toRadians(destinationPlace.longitude.toDouble() - originPlace.longitude.toDouble())
        val a = sin(deltaLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(deltaLng / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun ItineraryItem.toRouteItem(order: Int): TripReviewResult.RouteOptimizationItem =
        TripReviewResult.RouteOptimizationItem(
            itemId = id,
            itemOrder = order,
            name = displayName(),
            placeId = place?.id,
            externalPlaceId = place?.externalPlaceId,
        )

    private fun ItineraryItem.displayName(): String = place?.name ?: title ?: ""

    companion object {
        private const val DEFAULT_MODE = "WALKING"
        private const val MAX_OPTIMIZABLE_ITEMS_PER_DAY = 8
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}

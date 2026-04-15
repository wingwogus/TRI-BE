package com.tribe.application.itinerary.place

enum class NormalizedPlaceCategoryKey {
    KOREAN_FOOD,
    JAPANESE_FOOD,
    CHINESE_FOOD,
    RESTAURANT,
    CAFE,
    BAKERY,
    BAR,
    ATTRACTION,
    SHOPPING,
    STAY,
    PARK,
    MUSEUM,
    TRANSPORT,
    ETC,
}

object PlaceCategoryNormalizer {
    fun normalize(
        primaryType: String?,
        types: List<String>,
    ): NormalizedPlaceCategoryKey? {
        val candidates = buildList {
            primaryType?.let(::add)
            addAll(types)
        }.map { it.lowercase() }

        if (candidates.isEmpty()) {
            return null
        }

        return when {
            candidates.any { it in setOf("korean_restaurant") } -> NormalizedPlaceCategoryKey.KOREAN_FOOD
            candidates.any { it in setOf("japanese_restaurant", "ramen_restaurant", "sushi_restaurant") } -> NormalizedPlaceCategoryKey.JAPANESE_FOOD
            candidates.any { it in setOf("chinese_restaurant") } -> NormalizedPlaceCategoryKey.CHINESE_FOOD
            candidates.any { it in setOf("cafe", "coffee_shop", "tea_house") } -> NormalizedPlaceCategoryKey.CAFE
            candidates.any { it in setOf("bakery") } -> NormalizedPlaceCategoryKey.BAKERY
            candidates.any { it in setOf("bar", "pub", "night_club") } -> NormalizedPlaceCategoryKey.BAR
            candidates.any { it in setOf("tourist_attraction", "historical_place", "monument", "visitor_center", "amusement_park", "aquarium", "zoo") } -> NormalizedPlaceCategoryKey.ATTRACTION
            candidates.any { it in setOf("shopping_mall", "department_store", "store", "market", "clothing_store") } -> NormalizedPlaceCategoryKey.SHOPPING
            candidates.any { it in setOf("lodging", "hotel", "motel", "resort_hotel", "hostel") } -> NormalizedPlaceCategoryKey.STAY
            candidates.any { it in setOf("park", "national_park") } -> NormalizedPlaceCategoryKey.PARK
            candidates.any { it in setOf("museum", "art_gallery") } -> NormalizedPlaceCategoryKey.MUSEUM
            candidates.any { it in setOf("subway_station", "train_station", "airport", "bus_station", "transit_station") } -> NormalizedPlaceCategoryKey.TRANSPORT
            candidates.any { it in setOf("restaurant", "meal_takeaway", "meal_delivery", "food_court") } -> NormalizedPlaceCategoryKey.RESTAURANT
            else -> NormalizedPlaceCategoryKey.ETC
        }
    }
}

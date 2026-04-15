package com.tribe.application.itinerary.place

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class PlaceTypeSummary(
    val primaryType: String?,
    val types: List<String>,
    val displayPrimaryLabel: String?,
)

object PlaceTypeSummaryFactory {
    private val objectMapper = jacksonObjectMapper()

    fun fromRawTypes(primaryType: String?, types: List<String>): PlaceTypeSummary? {
        if (primaryType == null && types.isEmpty()) {
            return null
        }

        return PlaceTypeSummary(
            primaryType = primaryType,
            types = types,
            displayPrimaryLabel = toDisplayPrimaryLabel(primaryType),
        )
    }

    fun fromGoogleTypesJson(primaryType: String?, googleTypesJson: String?): PlaceTypeSummary? =
        fromRawTypes(primaryType, decodeGoogleTypes(googleTypesJson))

    fun decodeGoogleTypes(json: String?): List<String> =
        json?.let {
            runCatching { objectMapper.readValue(it, Array<String>::class.java).toList() }.getOrDefault(emptyList())
        } ?: emptyList()

    fun toDisplayPrimaryLabel(primaryType: String?): String? =
        primaryType?.replace('_', ' ')
}

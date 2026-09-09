package com.tribe.application.trip.ai

data class TripAiGenerationPayload(
    val title: String,
    val summary: String? = null,
    val warnings: List<String> = emptyList(),
    val days: List<TripAiGeneratedDay>,
)

data class TripAiGeneratedDay(
    val visitDay: Int,
    val theme: String? = null,
    val items: List<TripAiGeneratedItem>,
)

data class TripAiGeneratedItem(
    val order: Int,
    val time: String,
    val placeName: String,
    val searchQuery: String,
    val memo: String? = null,
    val durationMinutes: Int? = null,
    val styleTags: List<String> = emptyList(),
)

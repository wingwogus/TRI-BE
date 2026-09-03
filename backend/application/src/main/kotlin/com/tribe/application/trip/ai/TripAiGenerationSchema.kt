package com.tribe.application.trip.ai

object TripAiGenerationSchema {
    fun schema(): Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
            "title" to mapOf("type" to "string"),
            "summary" to mapOf("type" to "string"),
            "warnings" to mapOf(
                "type" to "array",
                "items" to mapOf("type" to "string"),
            ),
            "days" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "properties" to mapOf(
                        "visitDay" to mapOf("type" to "integer"),
                        "theme" to mapOf("type" to "string"),
                        "items" to mapOf(
                            "type" to "array",
                            "items" to mapOf(
                                "type" to "object",
                                "additionalProperties" to false,
                                "properties" to mapOf(
                                    "order" to mapOf("type" to "integer"),
                                    "time" to mapOf("type" to "string"),
                                    "placeName" to mapOf("type" to "string"),
                                    "searchQuery" to mapOf("type" to "string"),
                                    "memo" to mapOf("type" to "string"),
                                    "durationMinutes" to mapOf("type" to "integer"),
                                    "styleTags" to mapOf(
                                        "type" to "array",
                                        "items" to mapOf("type" to "string"),
                                    ),
                                ),
                                "required" to listOf("order", "time", "placeName", "searchQuery"),
                            ),
                            "minItems" to 1,
                            "maxItems" to TripAiGenerationPromptBuilder.MAX_ITEMS_PER_DAY,
                        ),
                    ),
                    "required" to listOf("visitDay", "items"),
                ),
            ),
        ),
        "required" to listOf("title", "days"),
    )
}

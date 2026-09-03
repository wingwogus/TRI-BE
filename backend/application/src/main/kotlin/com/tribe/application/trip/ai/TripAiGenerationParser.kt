package com.tribe.application.trip.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.springframework.stereotype.Component

@Component
class TripAiGenerationParser(
    private val objectMapper: ObjectMapper,
) {
    fun parse(content: String, expectedDayCount: Int): TripAiGenerationPayload {
        val payload = try {
            objectMapper.readValue(stripFence(content), TripAiGenerationPayload::class.java)
        } catch (_: Exception) {
            throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
        }
        validate(payload, expectedDayCount)
        return payload.copy(
            title = payload.title.trim().take(MAX_TITLE_LENGTH),
            summary = payload.summary?.trim()?.takeIf { it.isNotEmpty() },
            warnings = payload.warnings.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
            days = payload.days.map { day ->
                day.copy(
                    theme = day.theme?.trim()?.takeIf { it.isNotEmpty() },
                    items = day.items.sortedBy { it.order }.map { item ->
                        item.copy(
                            placeName = item.placeName.trim(),
                            searchQuery = item.searchQuery.trim(),
                            memo = item.memo?.trim()?.take(MAX_MEMO_LENGTH)?.takeIf { it.isNotEmpty() },
                            styleTags = item.styleTags.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
                        )
                    },
                )
            },
        )
    }

    fun toJson(payload: TripAiGenerationPayload): String =
        objectMapper.writeValueAsString(payload)

    private fun stripFence(content: String): String {
        val trimmed = content.trim()
        if (!trimmed.startsWith("```")) {
            return trimmed
        }
        val lines = trimmed.lines()
        val withoutOpening = lines.drop(1)
        val withoutClosing = if (withoutOpening.lastOrNull()?.trim() == "```") {
            withoutOpening.dropLast(1)
        } else {
            withoutOpening
        }
        return withoutClosing.joinToString("\n").trim()
    }

    private fun validate(payload: TripAiGenerationPayload, expectedDayCount: Int) {
        if (payload.title.isBlank() || payload.days.size != expectedDayCount) {
            throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
        }
        val dayNumbers = payload.days.map { it.visitDay }
        if (dayNumbers.toSet().size != dayNumbers.size || dayNumbers.any { it !in 1..expectedDayCount }) {
            throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
        }
        payload.days.forEach { day ->
            if (day.items.isEmpty() || day.items.size > TripAiGenerationPromptBuilder.MAX_ITEMS_PER_DAY) {
                throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
            }
            val orders = day.items.map { it.order }
            if (orders.toSet().size != orders.size || orders.any { it < 1 }) {
                throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
            }
            day.items.forEach { item ->
                if (!TIME_PATTERN.matches(item.time) || item.placeName.isBlank() || item.searchQuery.isBlank()) {
                    throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
                }
            }
        }
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 80
        private const val MAX_MEMO_LENGTH = 500
        private val TIME_PATTERN = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")
    }
}

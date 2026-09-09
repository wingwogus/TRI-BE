package com.tribe.application.trip.ai

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceSearchService
import com.tribe.application.security.CurrentActor
import com.tribe.domain.trip.ai.TripAiProposal
import com.tribe.domain.trip.ai.TripAiProposalRepository
import com.tribe.domain.trip.ai.TripAiProposalStatus
import com.tribe.domain.trip.ai.TripAiProposalType
import com.tribe.domain.trip.core.TripRegion
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@ConditionalOnProperty(name = ["tribe.trip.ai-generation.enabled"], havingValue = "true", matchIfMissing = true)
class TripAiGenerationService(
    private val currentActor: CurrentActor,
    private val geminiGateway: GeminiGateway,
    private val promptBuilder: TripAiGenerationPromptBuilder,
    private val parser: TripAiGenerationParser,
    private val proposalRepository: TripAiProposalRepository,
    private val placeSearchService: PlaceSearchService,
    private val applyService: TripAiGenerationApplyService,
) {
    fun createProposal(command: TripAiGenerationCommand.Create): TripAiGenerationResult.Proposal {
        val requesterId = currentActor.requireUserId()
        val region = resolveRegion(command.regionCode)
        val dayCount = validateDateRange(command.startDate, command.endDate)
        val companionType = resolveCompanionType(command.companionType)
        val travelStyles = resolveTravelStyles(command.travelStyles)
        val prompt = promptBuilder.build(region, command.startDate, command.endDate, dayCount, companionType, travelStyles)
        val aiResponse = geminiGateway.generateJson(prompt, TripAiGenerationSchema.schema())
            ?: throw BusinessException(ErrorCode.AI_FEEDBACK_ERROR)
        val payload = parser.parse(aiResponse, dayCount)
        val title = normalizeTitle(command.title, payload.title, region)
        val canonicalPayload = payload.copy(title = title)
        val proposal = proposalRepository.save(
            TripAiProposal(
                type = TripAiProposalType.CREATE_TRIP,
                status = TripAiProposalStatus.READY,
                requesterMemberId = requesterId,
                title = title,
                regionCode = region.code,
                countryCode = region.country.code,
                startDate = command.startDate,
                endDate = command.endDate,
                companionType = companionType.name,
                travelStyles = travelStyles.joinToString(",") { it.name },
                inputSnapshot = promptBuilder.normalizedInput(
                    region,
                    command.startDate,
                    command.endDate,
                    dayCount,
                    companionType,
                    travelStyles,
                ),
                aiResponseSnapshot = parser.toJson(canonicalPayload),
            ),
        )

        return toProposalResult(proposal, canonicalPayload)
    }

    fun getProposal(proposalId: Long): TripAiGenerationResult.Proposal {
        val proposal = findCurrentUserProposal(proposalId)
        return toProposalResult(proposal, parser.parse(proposal.aiResponseSnapshot, dayCount(proposal.startDate, proposal.endDate)))
    }

    fun applyProposal(command: TripAiGenerationCommand.Apply): TripAiGenerationResult.Applied {
        val proposal = findCurrentUserProposal(command.proposalId)
        if (proposal.status == TripAiProposalStatus.APPLIED) {
            return applyService.apply(command.proposalId, emptyList(), command.allowTextOnlyItems)
        }
        if (proposal.status != TripAiProposalStatus.READY) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        val region = resolveRegion(proposal.regionCode)
        val payload = parser.parse(proposal.aiResponseSnapshot, dayCount(proposal.startDate, proposal.endDate))
        val generatedItems = payload.days
            .sortedBy { it.visitDay }
            .flatMap { day ->
                day.items.sortedBy { it.order }.mapIndexed { index, item ->
                    GeneratedItem(day.visitDay, index + 1, item)
                }
            }
        val placesByQuery = generatedItems
            .map { it.item.searchQuery }
            .distinct()
            .associateWith { query -> resolvePlace(query, region) }
        val resolvedItems = generatedItems.map { generated ->
            val resolvedPlace = placesByQuery[generated.item.searchQuery]
            TripAiResolvedItem(
                visitDay = generated.visitDay,
                order = generated.order,
                placeId = resolvedPlace?.placeId,
                placeCandidate = resolvedPlace?.candidate,
                title = generated.item.placeName,
                time = toVisitDateTime(proposal.startDate, generated.visitDay, generated.item.time),
                memo = generated.item.memo,
            )
        }
        if (!command.allowTextOnlyItems && resolvedItems.any { !it.hasResolvedPlace }) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND)
        }
        return applyService.apply(command.proposalId, resolvedItems, command.allowTextOnlyItems)
    }

    private fun findCurrentUserProposal(proposalId: Long): TripAiProposal {
        val requesterId = currentActor.requireUserId()
        return proposalRepository.findByIdAndRequesterMemberId(proposalId, requesterId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
    }

    private fun resolveRegion(regionCode: String): TripRegion =
        TripRegion.from(regionCode)
            ?: throw BusinessException(
                ErrorCode.INVALID_INPUT,
                detail = mapOf("field" to "regionCode", "reason" to "Unsupported region code", "rejectedValue" to regionCode),
            )

    private fun validateDateRange(startDate: LocalDate, endDate: LocalDate): Int {
        if (startDate.isAfter(endDate)) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        val days = dayCount(startDate, endDate)
        if (days !in 1..MAX_TRIP_DAYS) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                detail = mapOf("field" to "endDate", "reason" to "Trip generation supports up to 4 nights 5 days"),
            )
        }
        return days
    }

    private fun resolveCompanionType(value: String): TripCompanionType =
        runCatching { TripCompanionType.valueOf(value.trim().uppercase()) }.getOrElse {
            throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("field" to "companionType", "rejectedValue" to value))
        }

    private fun resolveTravelStyles(values: List<String>): List<TripTravelStyle> {
        val styles = values.map { value ->
            runCatching { TripTravelStyle.valueOf(value.trim().uppercase()) }.getOrElse {
                throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("field" to "travelStyles", "rejectedValue" to value))
            }
        }
        if (styles.isEmpty() || styles.size > MAX_TRAVEL_STYLES || styles.toSet().size != styles.size) {
            throw BusinessException(ErrorCode.INVALID_INPUT, detail = mapOf("field" to "travelStyles"))
        }
        return styles
    }

    private fun normalizeTitle(requestTitle: String?, aiTitle: String, region: TripRegion): String {
        val title = requestTitle?.trim()?.takeIf { it.isNotEmpty() } ?: aiTitle.trim().takeIf { it.isNotEmpty() }
        return (title ?: "${region.label} AI 여행").take(MAX_TITLE_LENGTH)
    }

    private fun resolvePlace(searchQuery: String, region: TripRegion): ResolvedPlace? {
        val searchResult = placeSearchService.search(
            query = searchQuery,
            language = "ko",
            region = region.country.code,
            latitude = region.centerLat,
            longitude = region.centerLng,
            radiusMeters = 50_000,
            regionContextKey = "region:${region.code}",
        ).firstOrNull() ?: return null

        return ResolvedPlace(
            placeId = searchResult.placeId,
            candidate = if (searchResult.placeId == null) {
                TripAiPlaceCandidate(
                    externalPlaceId = searchResult.externalPlaceId,
                    placeName = searchResult.placeName,
                    address = searchResult.address,
                    latitude = BigDecimal.valueOf(searchResult.latitude),
                    longitude = BigDecimal.valueOf(searchResult.longitude),
                )
            } else {
                null
            },
        )
    }

    private fun toVisitDateTime(startDate: LocalDate, visitDay: Int, time: String): LocalDateTime =
        LocalDateTime.of(startDate.plusDays((visitDay - 1).toLong()), java.time.LocalTime.parse(time))

    private fun toProposalResult(
        proposal: TripAiProposal,
        payload: TripAiGenerationPayload,
    ): TripAiGenerationResult.Proposal =
        TripAiGenerationResult.Proposal(
            proposalId = proposal.id,
            status = proposal.status.name,
            title = proposal.title,
            summary = payload.summary,
            regionCode = proposal.regionCode,
            country = proposal.countryCode,
            startDate = proposal.startDate,
            endDate = proposal.endDate,
            companionType = proposal.companionType,
            travelStyles = proposal.travelStyles.split(",").filter { it.isNotBlank() },
            createdTripId = proposal.createdTripId,
            warnings = payload.warnings,
            days = payload.days.sortedBy { it.visitDay }.map { day ->
                TripAiGenerationResult.Day(
                    visitDay = day.visitDay,
                    theme = day.theme,
                    items = day.items.sortedBy { it.order }.map { item ->
                        TripAiGenerationResult.Item(
                            order = item.order,
                            time = item.time,
                            placeName = item.placeName,
                            searchQuery = item.searchQuery,
                            memo = item.memo,
                            durationMinutes = item.durationMinutes,
                            styleTags = item.styleTags,
                        )
                    },
                )
            },
        )

    private fun dayCount(startDate: LocalDate, endDate: LocalDate): Int =
        ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    private data class GeneratedItem(
        val visitDay: Int,
        val order: Int,
        val item: TripAiGeneratedItem,
    )

    private data class ResolvedPlace(
        val placeId: Long?,
        val candidate: TripAiPlaceCandidate?,
    )

    companion object {
        private const val MAX_TRIP_DAYS = 5
        private const val MAX_TRAVEL_STYLES = 3
        private const val MAX_TITLE_LENGTH = 80
    }
}

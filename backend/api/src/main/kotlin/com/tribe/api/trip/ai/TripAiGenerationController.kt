package com.tribe.api.trip.ai

import com.tribe.api.common.ApiResponse
import com.tribe.application.trip.ai.TripAiGenerationService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/ai-generations")
@ConditionalOnProperty(name = ["tribe.trip.ai-generation.enabled"], havingValue = "true", matchIfMissing = true)
class TripAiGenerationController(
    private val tripAiGenerationService: TripAiGenerationService,
) {
    @PostMapping
    fun createProposal(
        @Valid @RequestBody request: TripAiGenerationRequests.CreateRequest,
    ): ResponseEntity<ApiResponse<TripAiGenerationResponses.ProposalResponse>> {
        val result = tripAiGenerationService.createProposal(request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(TripAiGenerationResponses.ProposalResponse.from(result)))
    }

    @GetMapping("/{proposalId}")
    fun getProposal(
        @PathVariable proposalId: Long,
    ): ResponseEntity<ApiResponse<TripAiGenerationResponses.ProposalResponse>> {
        val result = tripAiGenerationService.getProposal(proposalId)
        return ResponseEntity.ok(ApiResponse.ok(TripAiGenerationResponses.ProposalResponse.from(result)))
    }

    @PostMapping("/{proposalId}/apply")
    fun applyProposal(
        @PathVariable proposalId: Long,
        @RequestBody request: TripAiGenerationRequests.ApplyRequest,
    ): ResponseEntity<ApiResponse<TripAiGenerationResponses.AppliedResponse>> {
        val result = tripAiGenerationService.applyProposal(request.toCommand(proposalId))
        return ResponseEntity.ok(ApiResponse.ok(TripAiGenerationResponses.AppliedResponse.from(result)))
    }
}

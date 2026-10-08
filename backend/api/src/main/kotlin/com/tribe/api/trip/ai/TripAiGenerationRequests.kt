package com.tribe.api.trip.ai

import com.tribe.application.trip.ai.TripAiGenerationCommand
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TripAiGenerationRequests {
    data class CreateRequest(
        @field:NotBlank(message = "여행지는 필수입니다.")
        val regionCode: String,
        @field:NotNull(message = "여행 시작일은 필수입니다.")
        val startDate: LocalDate,
        @field:NotNull(message = "여행 종료일은 필수입니다.")
        val endDate: LocalDate,
        val title: String? = null,
        @field:NotBlank(message = "동행자 유형은 필수입니다.")
        val companionType: String,
        @field:NotEmpty(message = "여행 스타일은 하나 이상 선택해야 합니다.")
        @field:Size(max = 3, message = "여행 스타일은 최대 3개까지 선택할 수 있습니다.")
        val travelStyles: List<String>,
    ) {
        @AssertTrue(message = "여행 시작일은 종료일보다 이전이거나 같아야 합니다.")
        fun isDatesValid(): Boolean = !startDate.isAfter(endDate)

        @AssertTrue(message = "AI 여행 생성은 최대 4박 5일까지 가능합니다.")
        fun isWithinAiGenerationRange(): Boolean =
            !startDate.isAfter(endDate) && ChronoUnit.DAYS.between(startDate, endDate) <= 4

        fun toCommand(): TripAiGenerationCommand.Create = TripAiGenerationCommand.Create(
            regionCode = regionCode,
            startDate = startDate,
            endDate = endDate,
            title = title,
            companionType = companionType,
            travelStyles = travelStyles,
        )
    }

    data class ApplyRequest(
        val allowTextOnlyItems: Boolean = true,
    ) {
        fun toCommand(proposalId: Long): TripAiGenerationCommand.Apply = TripAiGenerationCommand.Apply(
            proposalId = proposalId,
            allowTextOnlyItems = allowTextOnlyItems,
        )
    }
}

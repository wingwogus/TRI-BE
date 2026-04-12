package com.tribe.api.expense

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

object ExpenseRequests {
    data class ParticipantRequest(
        @field:NotNull(message = "참여자 ID는 필수입니다.")
        val tripMemberId: Long,
        @field:DecimalMin(value = "0.0", inclusive = false, message = "분담 금액은 0보다 커야 합니다.")
        val shareAmount: BigDecimal? = null,
    )

    data class CreateRequest(
        @field:NotBlank(message = "지출 제목은 필수입니다.")
        val title: String,
        @field:DecimalMin(value = "0.0", inclusive = false, message = "지출 금액은 0보다 커야 합니다.")
        val amount: BigDecimal,
        @field:NotBlank(message = "통화 코드는 필수입니다.")
        val currencyCode: String,
        @field:NotNull(message = "지출 일자는 필수입니다.")
        val spentAt: LocalDate,
        @field:NotBlank(message = "지출 카테고리는 필수입니다.")
        val category: String,
        @field:NotBlank(message = "정산 방식은 필수입니다.")
        val splitType: String,
        @field:NotNull(message = "결제자 ID는 필수입니다.")
        val payerTripMemberId: Long,
        val note: String? = null,
        @field:Valid
        val participants: List<ParticipantRequest> = emptyList(),
    )

    data class UpdateRequest(
        @field:NotBlank(message = "지출 제목은 필수입니다.")
        val title: String,
        @field:DecimalMin(value = "0.0", inclusive = false, message = "지출 금액은 0보다 커야 합니다.")
        val amount: BigDecimal,
        @field:NotBlank(message = "통화 코드는 필수입니다.")
        val currencyCode: String,
        @field:NotNull(message = "지출 일자는 필수입니다.")
        val spentAt: LocalDate,
        @field:NotBlank(message = "지출 카테고리는 필수입니다.")
        val category: String,
        @field:NotBlank(message = "정산 방식은 필수입니다.")
        val splitType: String,
        @field:NotNull(message = "결제자 ID는 필수입니다.")
        val payerTripMemberId: Long,
        val note: String? = null,
        @field:Valid
        val participants: List<ParticipantRequest> = emptyList(),
    )
}

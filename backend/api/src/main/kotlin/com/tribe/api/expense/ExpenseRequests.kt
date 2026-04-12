package com.tribe.api.expense

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

object ExpenseRequests {
    data class ItemRequest(
        val itemId: Long? = null,
        @field:NotBlank(message = "항목 이름은 필수입니다.")
        val itemName: String,
        @field:NotNull(message = "항목 가격은 필수입니다.")
        @field:DecimalMin(value = "0.0", inclusive = true, message = "항목 가격은 0 이상이어야 합니다.")
        val price: BigDecimal,
    )

    data class ItemAssignmentRequest(
        @field:NotNull(message = "항목 ID는 필수입니다.")
        val itemId: Long,
        @field:NotEmpty(message = "참여자 목록은 비어있을 수 없습니다.")
        val participantIds: List<Long>,
    )

    data class CreateRequest(
        @field:NotBlank(message = "지출 제목은 필수입니다.")
        val title: String,
        @field:DecimalMin(value = "0.0", inclusive = false, message = "지출 금액은 0보다 커야 합니다.")
        val amount: BigDecimal? = null,
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
        val itineraryItemId: Long? = null,
        @field:NotBlank(message = "입력 방식은 필수입니다.")
        val inputMethod: String,
        val note: String? = null,
        @field:Valid
        val items: List<ItemRequest> = emptyList(),
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
        val itineraryItemId: Long? = null,
        @field:NotBlank(message = "입력 방식은 필수입니다.")
        val inputMethod: String,
        val note: String? = null,
        @field:Valid
        val items: List<ItemRequest> = emptyList(),
    )

    data class AssignParticipantsRequest(
        @field:Valid
        @field:NotEmpty(message = "배정 항목은 비어있을 수 없습니다.")
        val items: List<ItemAssignmentRequest>,
    )

    data class ClearAssignmentsRequest(
        @field:NotEmpty(message = "삭제할 항목 ID는 비어있을 수 없습니다.")
        val itemIds: List<Long>,
    )
}

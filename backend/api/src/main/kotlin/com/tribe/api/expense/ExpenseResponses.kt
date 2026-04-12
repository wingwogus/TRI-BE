package com.tribe.api.expense

import com.tribe.application.expense.ExpenseResult
import java.math.BigDecimal
import java.time.LocalDate

object ExpenseResponses {
    data class ParticipantResponse(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val role: String,
        val shareAmount: BigDecimal?,
    ) {
        companion object {
            fun from(result: ExpenseResult.ParticipantSummary) = ParticipantResponse(
                tripMemberId = result.tripMemberId,
                memberId = result.memberId,
                nickname = result.nickname,
                role = result.role,
                shareAmount = result.shareAmount,
            )
        }
    }

    data class ExpenseSummaryResponse(
        val expenseId: Long,
        val tripId: Long,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val payerName: String,
        val participantCount: Int,
    ) {
        companion object {
            fun from(result: ExpenseResult.Summary) = ExpenseSummaryResponse(
                expenseId = result.expenseId,
                tripId = result.tripId,
                title = result.title,
                amount = result.amount,
                currencyCode = result.currencyCode,
                spentAt = result.spentAt,
                category = result.category,
                splitType = result.splitType,
                payerTripMemberId = result.payerTripMemberId,
                payerName = result.payerName,
                participantCount = result.participantCount,
            )
        }
    }

    data class ExpenseDetailResponse(
        val expenseId: Long,
        val tripId: Long,
        val createdByMemberId: Long,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val payerName: String,
        val note: String?,
        val participants: List<ParticipantResponse>,
    ) {
        companion object {
            fun from(result: ExpenseResult.Detail) = ExpenseDetailResponse(
                expenseId = result.expenseId,
                tripId = result.tripId,
                createdByMemberId = result.createdByMemberId,
                title = result.title,
                amount = result.amount,
                currencyCode = result.currencyCode,
                spentAt = result.spentAt,
                category = result.category,
                splitType = result.splitType,
                payerTripMemberId = result.payerTripMemberId,
                payerName = result.payerName,
                note = result.note,
                participants = result.participants.map(ParticipantResponse::from),
            )
        }
    }
}

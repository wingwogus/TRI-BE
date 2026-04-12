package com.tribe.application.expense

import com.tribe.domain.expense.Expense
import java.math.BigDecimal
import java.time.LocalDate

object ExpenseResult {
    data class ParticipantSummary(
        val tripMemberId: Long,
        val memberId: Long?,
        val nickname: String,
        val role: String,
        val shareAmount: BigDecimal?,
    )

    data class Summary(
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
            fun from(expense: Expense) = Summary(
                expenseId = expense.id,
                tripId = expense.trip.id,
                title = expense.title,
                amount = expense.amount,
                currencyCode = expense.currencyCode,
                spentAt = expense.spentAt,
                category = expense.category.name,
                splitType = expense.splitType.name,
                payerTripMemberId = expense.payer.id,
                payerName = expense.payer.name,
                participantCount = expense.participants.size,
            )
        }
    }

    data class Detail(
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
        val participants: List<ParticipantSummary>,
    ) {
        companion object {
            fun from(expense: Expense) = Detail(
                expenseId = expense.id,
                tripId = expense.trip.id,
                createdByMemberId = expense.createdBy.id,
                title = expense.title,
                amount = expense.amount,
                currencyCode = expense.currencyCode,
                spentAt = expense.spentAt,
                category = expense.category.name,
                splitType = expense.splitType.name,
                payerTripMemberId = expense.payer.id,
                payerName = expense.payer.name,
                note = expense.note,
                participants = expense.participants.map {
                    ParticipantSummary(
                        tripMemberId = it.tripMember.id,
                        memberId = it.tripMember.member?.id,
                        nickname = it.tripMember.name,
                        role = it.tripMember.role.name,
                        shareAmount = it.shareAmount,
                    )
                },
            )
        }
    }
}

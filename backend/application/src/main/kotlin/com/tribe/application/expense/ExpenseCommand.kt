package com.tribe.application.expense

import java.math.BigDecimal
import java.time.LocalDate

object ExpenseCommand {
    data class Participant(
        val tripMemberId: Long,
        val shareAmount: BigDecimal? = null,
    )

    data class Create(
        val tripId: Long,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val note: String? = null,
        val participants: List<Participant> = emptyList(),
    )

    data class Update(
        val tripId: Long,
        val expenseId: Long,
        val title: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val spentAt: LocalDate,
        val category: String,
        val splitType: String,
        val payerTripMemberId: Long,
        val note: String? = null,
        val participants: List<Participant> = emptyList(),
    )

    data class Delete(
        val tripId: Long,
        val expenseId: Long,
    )
}

package com.tribe.application.expense

object ExpenseQuery {
    data class ListByTrip(
        val tripId: Long,
    )

    data class GetDetail(
        val tripId: Long,
        val expenseId: Long,
    )
}

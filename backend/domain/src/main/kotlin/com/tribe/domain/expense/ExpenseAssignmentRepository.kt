package com.tribe.domain.expense

import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseAssignmentRepository : JpaRepository<ExpenseAssignment, Long> {
    fun deleteByExpenseItemId(itemId: Long)
    fun findAllByTripMemberId(tripMemberId: Long): List<ExpenseAssignment>
    fun deleteByExpenseItemIdAndTripMemberId(expenseItemId: Long, tripMemberId: Long)
}

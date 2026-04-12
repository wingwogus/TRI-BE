package com.tribe.domain.expense

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findAllByTripIdOrderBySpentAtDescIdDesc(tripId: Long): List<Expense>

    @Query(
        """
        select distinct e
        from Expense e
        join fetch e.trip
        join fetch e.createdBy
        join fetch e.payer payer
        left join fetch payer.member
        left join fetch e.participants participant
        left join fetch participant.tripMember tripMember
        left join fetch tripMember.member
        where e.id = :expenseId
        """
    )
    fun findWithDetailsById(@Param("expenseId") expenseId: Long): Expense?
}

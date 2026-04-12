package com.tribe.domain.expense

import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseItemRepository : JpaRepository<ExpenseItem, Long>

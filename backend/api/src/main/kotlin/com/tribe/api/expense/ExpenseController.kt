package com.tribe.api.expense

import com.tribe.api.common.ApiResponse
import com.tribe.application.expense.CreateExpenseUseCase
import com.tribe.application.expense.DeleteExpenseUseCase
import com.tribe.application.expense.ExpenseCommand
import com.tribe.application.expense.ExpenseQuery
import com.tribe.application.expense.ExpenseResult
import com.tribe.application.expense.GetExpenseDetailUseCase
import com.tribe.application.expense.ListExpensesUseCase
import com.tribe.application.expense.UpdateExpenseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses")
class ExpenseController(
    private val createExpenseUseCase: CreateExpenseUseCase,
    private val listExpensesUseCase: ListExpensesUseCase,
    private val getExpenseDetailUseCase: GetExpenseDetailUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
) {
    @PostMapping
    fun createExpense(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: ExpenseRequests.CreateRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = createExpenseUseCase.createExpense(request.toCreateCommand(tripId))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @GetMapping
    fun listExpenses(
        @PathVariable tripId: Long,
    ): ResponseEntity<ApiResponse<List<ExpenseResponses.ExpenseSummaryResponse>>> {
        val result = listExpensesUseCase.listExpenses(ExpenseQuery.ListByTrip(tripId))
        return ResponseEntity.ok(ApiResponse.ok(result.map(ExpenseResponses.ExpenseSummaryResponse::from)))
    }

    @GetMapping("/{expenseId}")
    fun getExpenseDetail(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = getExpenseDetailUseCase.getExpenseDetail(ExpenseQuery.GetDetail(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @PatchMapping("/{expenseId}")
    fun updateExpense(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: ExpenseRequests.UpdateRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = updateExpenseUseCase.updateExpense(request.toUpdateCommand(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @DeleteMapping("/{expenseId}")
    fun deleteExpense(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        deleteExpenseUseCase.deleteExpense(ExpenseCommand.Delete(tripId, expenseId))
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }

    private fun ExpenseRequests.CreateRequest.toCreateCommand(tripId: Long) = ExpenseCommand.Create(
        tripId = tripId,
        title = title,
        amount = amount,
        currencyCode = currencyCode,
        spentAt = spentAt,
        category = category,
        splitType = splitType,
        payerTripMemberId = payerTripMemberId,
        note = note,
        participants = participants.map {
            ExpenseCommand.Participant(
                tripMemberId = it.tripMemberId,
                shareAmount = it.shareAmount,
            )
        },
    )

    private fun ExpenseRequests.UpdateRequest.toUpdateCommand(tripId: Long, expenseId: Long) = ExpenseCommand.Update(
        tripId = tripId,
        expenseId = expenseId,
        title = title,
        amount = amount,
        currencyCode = currencyCode,
        spentAt = spentAt,
        category = category,
        splitType = splitType,
        payerTripMemberId = payerTripMemberId,
        note = note,
        participants = participants.map {
            ExpenseCommand.Participant(
                tripMemberId = it.tripMemberId,
                shareAmount = it.shareAmount,
            )
        },
    )
}

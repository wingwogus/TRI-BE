package com.tribe.api.expense

import com.tribe.api.common.ApiResponse
import com.tribe.application.expense.AssignExpenseParticipantsUseCase
import com.tribe.application.expense.ClearExpenseAssignmentsUseCase
import com.tribe.application.expense.CreateExpenseUseCase
import com.tribe.application.expense.DeleteExpenseUseCase
import com.tribe.application.expense.ExpenseCommand
import com.tribe.application.expense.ExpenseQuery
import com.tribe.application.expense.GetExpenseDetailUseCase
import com.tribe.application.expense.ListExpensesUseCase
import com.tribe.application.expense.UpdateExpenseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses")
class ExpenseController(
    private val createExpenseUseCase: CreateExpenseUseCase,
    private val listExpensesUseCase: ListExpensesUseCase,
    private val getExpenseDetailUseCase: GetExpenseDetailUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val assignExpenseParticipantsUseCase: AssignExpenseParticipantsUseCase,
    private val clearExpenseAssignmentsUseCase: ClearExpenseAssignmentsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createExpense(
        @PathVariable tripId: Long,
        @Valid @RequestPart("request") request: ExpenseRequests.CreateRequest,
        @RequestPart(value = "image", required = false) imageFile: MultipartFile?,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = createExpenseUseCase.createExpense(request.toCreateCommand(tripId, imageFile))
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

    @PostMapping("/{expenseId}/assignments")
    fun assignParticipants(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: ExpenseRequests.AssignParticipantsRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = assignExpenseParticipantsUseCase.assignParticipants(
            ExpenseCommand.AssignParticipants(
                tripId = tripId,
                expenseId = expenseId,
                items = request.items.map {
                    ExpenseCommand.ItemAssignment(
                        itemId = it.itemId,
                        participantIds = it.participantIds,
                    )
                },
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok(ExpenseResponses.ExpenseDetailResponse.from(result)))
    }

    @PostMapping("/{expenseId}/assignments:clear")
    fun clearAssignments(
        @PathVariable tripId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: ExpenseRequests.ClearAssignmentsRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponses.ExpenseDetailResponse>> {
        val result = clearExpenseAssignmentsUseCase.clearAssignments(
            ExpenseCommand.ClearAssignments(
                tripId = tripId,
                expenseId = expenseId,
                itemIds = request.itemIds,
            ),
        )
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

    private fun ExpenseRequests.CreateRequest.toCreateCommand(tripId: Long, imageFile: MultipartFile?) = ExpenseCommand.Create(
        tripId = tripId,
        title = title,
        amount = amount,
        currencyCode = currencyCode,
        spentAt = spentAt,
        category = category,
        splitType = splitType,
        payerTripMemberId = payerTripMemberId,
        itineraryItemId = itineraryItemId,
        inputMethod = inputMethod,
        note = note,
        items = items.map {
            ExpenseCommand.Item(
                itemId = it.itemId,
                itemName = it.itemName,
                price = it.price,
            )
        },
        receiptImageBytes = imageFile?.bytes,
        receiptImageContentType = imageFile?.contentType,
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
        itineraryItemId = itineraryItemId,
        inputMethod = inputMethod,
        note = note,
        items = items.map {
            ExpenseCommand.Item(
                itemId = it.itemId,
                itemName = it.itemName,
                price = it.price,
            )
        },
    )
}

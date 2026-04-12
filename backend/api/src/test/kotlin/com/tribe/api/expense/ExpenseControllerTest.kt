package com.tribe.api.expense

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.expense.CreateExpenseUseCase
import com.tribe.application.expense.DeleteExpenseUseCase
import com.tribe.application.expense.ExpenseCommand
import com.tribe.application.expense.ExpenseQuery
import com.tribe.application.expense.ExpenseResult
import com.tribe.application.expense.GetExpenseDetailUseCase
import com.tribe.application.expense.ListExpensesUseCase
import com.tribe.application.expense.UpdateExpenseUseCase
import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(ExpenseController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ExpenseControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var createExpenseUseCase: CreateExpenseUseCase
    @MockBean private lateinit var listExpensesUseCase: ListExpensesUseCase
    @MockBean private lateinit var getExpenseDetailUseCase: GetExpenseDetailUseCase
    @MockBean private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    @MockBean private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `createExpense returns created payload`() {
        `when`(
            createExpenseUseCase.createExpense(
                ExpenseCommand.Create(
                    tripId = 5L,
                    title = "Dinner",
                    amount = BigDecimal("120.50"),
                    currencyCode = "USD",
                    spentAt = LocalDate.of(2026, 4, 12),
                    category = "FOOD",
                    splitType = "EQUAL",
                    payerTripMemberId = 11L,
                    note = "Team meal",
                    participants = listOf(
                        ExpenseCommand.Participant(11L, BigDecimal("60.25")),
                        ExpenseCommand.Participant(12L, BigDecimal("60.25")),
                    ),
                ),
            ),
        ).thenReturn(sampleExpenseDetail())

        mockMvc.perform(
            post("/api/v1/trips/5/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Dinner",
                      "amount": 120.50,
                      "currencyCode": "USD",
                      "spentAt": "2026-04-12",
                      "category": "FOOD",
                      "splitType": "EQUAL",
                      "payerTripMemberId": 11,
                      "note": "Team meal",
                      "participants": [
                        {"tripMemberId": 11, "shareAmount": 60.25},
                        {"tripMemberId": 12, "shareAmount": 60.25}
                      ]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title", equalTo("Dinner")))
            .andExpect(jsonPath("$.data.participants[1].tripMemberId", equalTo(12)))
    }

    @Test
    fun `createExpense rejects blank title`() {
        mockMvc.perform(
            post("/api/v1/trips/5/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": " ",
                      "amount": 120.50,
                      "currencyCode": "USD",
                      "spentAt": "2026-04-12",
                      "category": "FOOD",
                      "splitType": "EQUAL",
                      "payerTripMemberId": 11
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("title")))
    }

    @Test
    fun `listExpenses returns summary collection`() {
        `when`(listExpensesUseCase.listExpenses(ExpenseQuery.ListByTrip(5L))).thenReturn(
            listOf(
                ExpenseResult.Summary(
                    expenseId = 1L,
                    tripId = 5L,
                    title = "Taxi",
                    amount = BigDecimal("32.00"),
                    currencyCode = "JPY",
                    spentAt = LocalDate.of(2026, 4, 12),
                    category = "TRANSPORT",
                    splitType = "EQUAL",
                    payerTripMemberId = 11L,
                    payerName = "payer",
                    participantCount = 2,
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/trips/5/expenses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].expenseId", equalTo(1)))
            .andExpect(jsonPath("$.data[0].payerName", equalTo("payer")))
    }

    private fun sampleExpenseDetail() = ExpenseResult.Detail(
        expenseId = 1L,
        tripId = 5L,
        createdByMemberId = 1L,
        title = "Dinner",
        amount = BigDecimal("120.50"),
        currencyCode = "USD",
        spentAt = LocalDate.of(2026, 4, 12),
        category = "FOOD",
        splitType = "EQUAL",
        payerTripMemberId = 11L,
        payerName = "payer",
        note = "Team meal",
        participants = listOf(
            ExpenseResult.ParticipantSummary(11L, 1L, "payer", "MEMBER", BigDecimal("60.25")),
            ExpenseResult.ParticipantSummary(12L, 2L, "member", "MEMBER", BigDecimal("60.25")),
        ),
    )
}

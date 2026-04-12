package com.tribe.api.expense

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.expense.AssignExpenseParticipantsUseCase
import com.tribe.application.expense.ClearExpenseAssignmentsUseCase
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
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
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
    @MockBean private lateinit var assignExpenseParticipantsUseCase: AssignExpenseParticipantsUseCase
    @MockBean private lateinit var clearExpenseAssignmentsUseCase: ClearExpenseAssignmentsUseCase
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
                    itineraryItemId = 7L,
                    inputMethod = "HANDWRITE",
                    note = "Team meal",
                    items = listOf(
                        ExpenseCommand.Item(itemName = "Pasta", price = BigDecimal("60.25")),
                        ExpenseCommand.Item(itemName = "Wine", price = BigDecimal("60.25")),
                    ),
                    receiptImageBytes = null,
                    receiptImageContentType = null,
                ),
            ),
        ).thenReturn(sampleExpenseDetail())

        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {
              "title": "Dinner",
              "amount": 120.50,
              "currencyCode": "USD",
              "spentAt": "2026-04-12",
              "category": "FOOD",
              "splitType": "EQUAL",
              "payerTripMemberId": 11,
              "itineraryItemId": 7,
              "inputMethod": "HANDWRITE",
              "note": "Team meal",
              "items": [
                {"itemName": "Pasta", "price": 60.25},
                {"itemName": "Wine", "price": 60.25}
              ]
            }
            """.trimIndent().toByteArray(),
        )
        mockMvc.perform(
            multipart("/api/v1/trips/5/expenses")
                .file(requestPart)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title", equalTo("Dinner")))
            .andExpect(jsonPath("$.data.items[0].itemName", equalTo("Pasta")))
    }

    @Test
    fun `createExpense rejects blank title`() {
        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {
              "title": " ",
              "amount": 120.50,
              "currencyCode": "USD",
              "spentAt": "2026-04-12",
              "category": "FOOD",
              "splitType": "EQUAL",
              "payerTripMemberId": 11,
              "inputMethod": "HANDWRITE",
              "items": [{"itemName": "Pasta", "price": 120.50}]
            }
            """.trimIndent().toByteArray(),
        )

        mockMvc.perform(multipart("/api/v1/trips/5/expenses").file(requestPart))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
    }

    @Test
    fun `assignParticipants returns updated detail`() {
        `when`(
            assignExpenseParticipantsUseCase.assignParticipants(
                ExpenseCommand.AssignParticipants(
                    tripId = 5L,
                    expenseId = 3L,
                    items = listOf(
                        ExpenseCommand.ItemAssignment(9L, listOf(11L, 12L)),
                    ),
                ),
            ),
        ).thenReturn(sampleExpenseDetail())

        mockMvc.perform(
            post("/api/v1/trips/5/expenses/3/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"items":[{"itemId":9,"participantIds":[11,12]}]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].participants[0].tripMemberId", equalTo(11)))
    }

    @Test
    fun `listExpenses returns summary collection`() {
        `when`(listExpensesUseCase.listExpenses(ExpenseQuery.ListByTrip(5L))).thenReturn(
            listOf(
                ExpenseResult.Summary(
                    expenseId = 1L,
                    tripId = 5L,
                    itineraryItemId = 7L,
                    title = "Taxi",
                    amount = BigDecimal("32.00"),
                    currencyCode = "JPY",
                    spentAt = LocalDate.of(2026, 4, 12),
                    category = "TRANSPORT",
                    splitType = "EQUAL",
                    payerTripMemberId = 11L,
                    payerName = "payer",
                    itemCount = 1,
                    inputMethod = "HANDWRITE",
                    receiptImageUrl = null,
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/trips/5/expenses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].expenseId", equalTo(1)))
            .andExpect(jsonPath("$.data[0].payerName", equalTo("payer")))
            .andExpect(jsonPath("$.data[0].itemCount", equalTo(1)))
    }

    private fun sampleExpenseDetail() = ExpenseResult.Detail(
        expenseId = 1L,
        tripId = 5L,
        itineraryItemId = 7L,
        createdByMemberId = 1L,
        title = "Dinner",
        amount = BigDecimal("120.50"),
        currencyCode = "USD",
        spentAt = LocalDate.of(2026, 4, 12),
        category = "FOOD",
        splitType = "EQUAL",
        inputMethod = "HANDWRITE",
        payerTripMemberId = 11L,
        payerName = "payer",
        note = "Team meal",
        receiptImageUrl = "https://cdn/receipt.jpg",
        items = listOf(
            ExpenseResult.ItemDetail(
                itemId = 9L,
                itemName = "Pasta",
                price = BigDecimal("60.25"),
                participants = listOf(
                    ExpenseResult.ItemParticipantSummary(11L, 1L, "payer", false, BigDecimal("30.12")),
                    ExpenseResult.ItemParticipantSummary(12L, 2L, "member", false, BigDecimal("30.13")),
                ),
            ),
        ),
    )
}

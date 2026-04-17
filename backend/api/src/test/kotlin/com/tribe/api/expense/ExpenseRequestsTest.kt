package com.tribe.api.expense

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseRequestsTest {
    @Test
    fun `create request maps multipart metadata into command`() {
        val request = ExpenseRequests.CreateRequest(
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
                ExpenseRequests.ItemRequest(itemId = 9L, itemName = "Pasta", price = BigDecimal("60.25")),
            ),
        )
        val image = MockMultipartFile(
            "image",
            "receipt.png",
            MediaType.IMAGE_PNG_VALUE,
            byteArrayOf(1, 2, 3),
        )

        val command = request.toCommand(5L, image)

        assertEquals(5L, command.tripId)
        assertEquals("Dinner", command.title)
        assertEquals("USD", command.currencyCode)
        assertEquals(7L, command.itineraryItemId)
        assertEquals(9L, command.items.single().itemId)
        assertEquals("Pasta", command.items.single().itemName)
        assertEquals("image/png", command.receiptImageContentType)
        assertArrayEquals(byteArrayOf(1, 2, 3), command.receiptImageBytes)
    }

    @Test
    fun `assign participants request maps nested assignments`() {
        val request = ExpenseRequests.AssignParticipantsRequest(
            items = listOf(
                ExpenseRequests.ItemAssignmentRequest(itemId = 9L, participantIds = listOf(11L, 12L)),
            ),
        )

        val command = request.toCommand(tripId = 5L, expenseId = 3L)

        assertEquals(5L, command.tripId)
        assertEquals(3L, command.expenseId)
        assertEquals(9L, command.items.single().itemId)
        assertEquals(listOf(11L, 12L), command.items.single().participantIds)
    }
}

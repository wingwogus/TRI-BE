package com.tribe.api.itinerary

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.itinerary.ItemCommand
import com.tribe.application.itinerary.ItemResult
import com.tribe.application.itinerary.ItemService
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
import java.time.LocalDateTime

@WebMvcTest(ItemController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ItemControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockBean private lateinit var itemService: ItemService
    @MockBean private lateinit var tokenProvider: TokenProvider

    @Test
    fun `createItem returns created payload`() {
        `when`(
            itemService.createItem(
                ItemCommand.Create(
                    tripId = 5L,
                    categoryId = 11L,
                    title = "Dinner",
                    time = LocalDateTime.of(2026, 4, 12, 19, 0),
                    memo = "Booked",
                ),
            ),
        ).thenReturn(sampleItemView())

        mockMvc.perform(
            post("/api/v1/trips/5/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": 11,
                      "title": "Dinner",
                      "time": "2026-04-12T19:00:00",
                      "memo": "Booked"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.itemId", equalTo(1)))
            .andExpect(jsonPath("$.data.categoryId", equalTo(11)))
    }

    @Test
    fun `getAllItems returns collection`() {
        `when`(itemService.getAllItems(5L, null)).thenReturn(listOf(sampleItemView()))

        mockMvc.perform(get("/api/v1/trips/5/items"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].itemId", equalTo(1)))
            .andExpect(jsonPath("$.data[0].title", equalTo("Dinner")))
    }

    private fun sampleItemView() = ItemResult.ItemView(
        itemId = 1L,
        categoryId = 11L,
        categoryName = "Meals",
        tripId = 5L,
        day = 1,
        title = "Dinner",
        time = LocalDateTime.of(2026, 4, 12, 19, 0),
        order = 3,
        memo = "Booked",
    )
}

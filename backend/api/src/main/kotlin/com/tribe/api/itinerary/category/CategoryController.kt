package com.tribe.api.itinerary.category

import com.tribe.api.common.ApiResponse
import com.tribe.application.itinerary.category.CategoryCommand
import com.tribe.application.itinerary.category.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/categories")
class CategoryController(
    private val categoryService: CategoryService,
) {
    @PostMapping
    fun createCategory(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: CategoryRequests.CreateRequest,
    ): ResponseEntity<ApiResponse<CategoryResponses.CategoryResponse>> {
        val result = categoryService.createCategory(CategoryCommand.Create(tripId, request.name, request.day, request.order))
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(CategoryResponses.CategoryResponse.from(result)))
    }

    @GetMapping
    fun getAllCategories(
        @PathVariable tripId: Long,
        @RequestParam(required = false) day: Int?,
    ): ResponseEntity<ApiResponse<List<CategoryResponses.CategoryResponse>>> {
        val result = categoryService.getAllCategories(tripId, day).map(CategoryResponses.CategoryResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping("/{categoryId}")
    fun getCategory(
        @PathVariable tripId: Long,
        @PathVariable categoryId: Long,
    ): ResponseEntity<ApiResponse<CategoryResponses.CategoryResponse>> {
        val result = categoryService.getCategory(tripId, categoryId)
        return ResponseEntity.ok(ApiResponse.ok(CategoryResponses.CategoryResponse.from(result)))
    }

    @PatchMapping("/{categoryId}")
    fun updateCategory(
        @PathVariable tripId: Long,
        @PathVariable categoryId: Long,
        @RequestBody request: CategoryRequests.UpdateRequest,
    ): ResponseEntity<ApiResponse<CategoryResponses.CategoryResponse>> {
        val result = categoryService.updateCategory(
            CategoryCommand.Update(tripId, categoryId, request.name, request.day, request.order, request.memo),
        )
        return ResponseEntity.ok(ApiResponse.ok(CategoryResponses.CategoryResponse.from(result)))
    }

    @DeleteMapping("/{categoryId}")
    fun deleteCategory(
        @PathVariable tripId: Long,
        @PathVariable categoryId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        categoryService.deleteCategory(tripId, categoryId)
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }

    @PatchMapping("/order")
    fun orderUpdateCategory(
        @PathVariable tripId: Long,
        @Valid @RequestBody request: CategoryRequests.OrderUpdateRequest,
    ): ResponseEntity<ApiResponse<List<CategoryResponses.CategoryResponse>>> {
        val result = categoryService.orderUpdateCategory(
            CategoryCommand.OrderUpdate(
                tripId,
                request.items.map { CategoryCommand.OrderItem(it.categoryId, it.order) },
            )
        ).map(CategoryResponses.CategoryResponse::from)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}

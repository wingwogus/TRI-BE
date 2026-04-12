package com.tribe.application.itinerary

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.trip.TripAuthorizationPolicy
import com.tribe.domain.itinerary.Category
import com.tribe.domain.itinerary.CategoryRepository
import com.tribe.domain.itinerary.ItineraryItem
import com.tribe.domain.itinerary.ItineraryItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ItemService(
    private val categoryRepository: CategoryRepository,
    private val itineraryItemRepository: ItineraryItemRepository,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    fun createItem(command: ItemCommand.Create): ItemResult.ItemView {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val category = findCategory(command.tripId, command.categoryId)
        val item = itineraryItemRepository.save(
            ItineraryItem(
                category = category,
                place = null,
                title = normalizeNullableText(command.title),
                time = command.time,
                order = itineraryItemRepository.countByCategoryId(category.id) + 1,
                memo = normalizeNullableText(command.memo),
            ),
        )
        return ItemResult.ItemView.from(item)
    }

    @Transactional(readOnly = true)
    fun getItem(tripId: Long, itemId: Long): ItemResult.ItemView {
        tripAuthorizationPolicy.isTripMember(tripId)
        return ItemResult.ItemView.from(findItem(tripId, itemId))
    }

    @Transactional(readOnly = true)
    fun getAllItems(tripId: Long, categoryId: Long?): List<ItemResult.ItemView> {
        tripAuthorizationPolicy.isTripMember(tripId)
        return if (categoryId != null) {
            val category = findCategory(tripId, categoryId)
            itineraryItemRepository.findByCategoryIdOrderByOrderAsc(category.id)
                .map(ItemResult.ItemView::from)
        } else {
            categoryRepository.findAllByTripIdOrderByDayAscOrderAsc(tripId)
                .flatMap { category ->
                    itineraryItemRepository.findByCategoryIdOrderByOrderAsc(category.id)
                        .map(ItemResult.ItemView::from)
                }
        }
    }

    fun updateItem(command: ItemCommand.Update): ItemResult.ItemView {
        tripAuthorizationPolicy.isTripMember(command.tripId)
        val item = findItem(command.tripId, command.itemId)
        val targetCategory = command.categoryId?.let { findCategory(command.tripId, it) }

        if (targetCategory != null && targetCategory.id != item.category.id) {
            item.category = targetCategory
            item.order = itineraryItemRepository.countByCategoryId(targetCategory.id) + 1
        }
        command.title?.let { item.title = normalizeNullableText(it) }
        command.time?.let { item.time = it }
        command.memo?.let { item.memo = normalizeNullableText(it) }

        return ItemResult.ItemView.from(item)
    }

    fun deleteItem(tripId: Long, itemId: Long) {
        tripAuthorizationPolicy.isTripMember(tripId)
        itineraryItemRepository.delete(findItem(tripId, itemId))
    }

    private fun findCategory(tripId: Long, categoryId: Long): Category {
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
        if (category.trip.id != tripId) {
            throw BusinessException(ErrorCode.NO_BELONG_TRIP)
        }
        return category
    }

    private fun findItem(tripId: Long, itemId: Long): ItineraryItem {
        val item = itineraryItemRepository.findById(itemId)
            .orElseThrow { BusinessException(ErrorCode.ITEM_NOT_FOUND) }
        if (item.category.trip.id != tripId) {
            throw BusinessException(ErrorCode.NO_BELONG_TRIP)
        }
        return item
    }

    private fun normalizeNullableText(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}

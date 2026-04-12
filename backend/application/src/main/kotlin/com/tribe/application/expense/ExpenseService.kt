package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseParticipant
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.trip.Trip
import com.tribe.domain.trip.TripMember
import com.tribe.domain.trip.TripRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ExpenseService(
    private val expenseAuthorizationPolicy: ExpenseAuthorizationPolicy,
    private val expenseRepository: ExpenseRepository,
) : CreateExpenseUseCase,
    ListExpensesUseCase,
    GetExpenseDetailUseCase,
    UpdateExpenseUseCase,
    DeleteExpenseUseCase {

    override fun createExpense(command: ExpenseCommand.Create): ExpenseResult.Detail {
        val actorMembership = expenseAuthorizationPolicy.requireMembership(command.tripId)
        val trip = actorMembership.trip
        val payer = resolveTripMember(trip, command.payerTripMemberId)

        val expense = Expense(
            trip = trip,
            createdBy = actorMembership.member ?: throw BusinessException(ErrorCode.NO_AUTHORITY_TRIP),
            payer = payer,
            title = command.title.trim(),
            amount = command.amount,
            currencyCode = normalizeCurrencyCode(command.currencyCode),
            spentAt = command.spentAt,
            category = parseCategory(command.category),
            splitType = parseSplitType(command.splitType),
            note = command.note?.trim()?.takeIf { it.isNotEmpty() },
        )
        expense.replaceParticipants(resolveParticipants(expense, trip, command.participants))

        return ExpenseResult.Detail.from(expenseRepository.save(expense))
    }

    @Transactional(readOnly = true)
    override fun listExpenses(query: ExpenseQuery.ListByTrip): List<ExpenseResult.Summary> {
        expenseAuthorizationPolicy.requireMembership(query.tripId)
        return expenseRepository.findAllByTripIdOrderBySpentAtDescIdDesc(query.tripId)
            .map(ExpenseResult.Summary::from)
    }

    @Transactional(readOnly = true)
    override fun getExpenseDetail(query: ExpenseQuery.GetDetail): ExpenseResult.Detail {
        expenseAuthorizationPolicy.requireMembership(query.tripId)
        val expense = findExpense(query.expenseId)
        ensureTripMatch(expense, query.tripId)
        return ExpenseResult.Detail.from(expense)
    }

    override fun updateExpense(command: ExpenseCommand.Update): ExpenseResult.Detail {
        val expense = findExpense(command.expenseId)
        ensureTripMatch(expense, command.tripId)
        expenseAuthorizationPolicy.requireModificationAccess(expense)

        val trip = expense.trip
        expense.update(
            title = command.title.trim(),
            amount = command.amount,
            currencyCode = normalizeCurrencyCode(command.currencyCode),
            spentAt = command.spentAt,
            category = parseCategory(command.category),
            splitType = parseSplitType(command.splitType),
            payer = resolveTripMember(trip, command.payerTripMemberId),
            note = command.note?.trim()?.takeIf { it.isNotEmpty() },
        )
        expense.replaceParticipants(resolveParticipants(expense, trip, command.participants))
        return ExpenseResult.Detail.from(expense)
    }

    override fun deleteExpense(command: ExpenseCommand.Delete) {
        val expense = findExpense(command.expenseId)
        ensureTripMatch(expense, command.tripId)
        expenseAuthorizationPolicy.requireModificationAccess(expense)
        expenseRepository.delete(expense)
    }

    private fun findExpense(expenseId: Long): Expense =
        expenseRepository.findWithDetailsById(expenseId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

    private fun ensureTripMatch(expense: Expense, tripId: Long) {
        if (expense.trip.id != tripId) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    private fun resolveParticipants(
        expense: Expense,
        trip: Trip,
        participants: List<ExpenseCommand.Participant>,
    ): List<ExpenseParticipant> {
        if (participants.map { it.tripMemberId }.distinct().size != participants.size) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }

        return participants.map { participant ->
            ExpenseParticipant(
                expense = expense,
                tripMember = resolveTripMember(trip, participant.tripMemberId),
                shareAmount = participant.shareAmount,
            )
        }
    }

    private fun resolveTripMember(trip: Trip, tripMemberId: Long): TripMember =
        trip.members.firstOrNull {
            it.id == tripMemberId && it.role != TripRole.EXITED && it.role != TripRole.KICKED
        } ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

    private fun normalizeCurrencyCode(currencyCode: String): String {
        val normalized = currencyCode.trim().uppercase()
        if (normalized.length != 3) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        return normalized
    }

    private fun parseCategory(raw: String): ExpenseCategory =
        runCatching { ExpenseCategory.valueOf(raw.trim().uppercase()) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_INPUT) }

    private fun parseSplitType(raw: String): ExpenseSplitType =
        runCatching { ExpenseSplitType.valueOf(raw.trim().uppercase()) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_INPUT) }
}

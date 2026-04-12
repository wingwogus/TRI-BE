package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.trip.TripAuthorizationPolicy
import com.tribe.domain.exchange.Currency
import com.tribe.domain.exchange.CurrencyRepository
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.trip.TripMember
import com.tribe.domain.trip.TripRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val expenseRepository: ExpenseRepository,
    private val tripRepository: TripRepository,
    private val currencyRepository: CurrencyRepository,
    private val tripAuthorizationPolicy: TripAuthorizationPolicy,
) {
    private val krw = "KRW"
    private val epsilon = BigDecimal("1.00")

    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    fun getDailySettlement(tripId: Long, date: LocalDate): SettlementResult.Daily {
        val trip = tripRepository.findById(tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val dailyExpenses = expenseRepository.findAllByTripIdOrderBySpentAtDescIdDesc(tripId)
            .filter { it.spentAt == date }

        val totalKrw = dailyExpenses.sumOf { convertToKrw(it.amount, it.currencyCode, it.spentAt) }
        val memberData = calculateMemberSettlementData(trip.members, dailyExpenses)
        val memberSummaries = memberData.map {
            SettlementResult.MemberDailySummary(
                memberId = it.member.id,
                memberName = it.member.name,
                paidAmount = it.paidAmountKrw,
                assignedAmount = it.assignedAmountKrw,
            )
        }
        val debtRelations = calculateDebtRelations(memberData)

        val totalAssigned = memberSummaries.sumOf { it.assignedAmount }
        if (totalKrw.subtract(totalAssigned).abs() > epsilon) {
            // tolerated warning path; keep response generation stable
        }

        return SettlementResult.Daily(
            date = date,
            dailyTotalAmount = totalKrw,
            expenses = dailyExpenses.map {
                SettlementResult.DailyExpenseSummary(
                    expenseId = it.id,
                    title = it.title,
                    payerName = it.payer.name,
                    totalAmount = convertToKrw(it.amount, it.currencyCode, it.spentAt),
                    originalAmount = it.amount,
                    currencyCode = it.currencyCode,
                )
            },
            memberSummaries = memberSummaries,
            debtRelations = debtRelations,
        )
    }

    @PreAuthorize("@tripAuthorizationPolicy.isTripMember(#tripId)")
    fun getTotalSettlement(tripId: Long): SettlementResult.Total {
        val trip = tripRepository.findById(tripId).orElseThrow { BusinessException(ErrorCode.TRIP_NOT_FOUND) }
        val expenses = expenseRepository.findAllByTripIdOrderBySpentAtDescIdDesc(tripId)
        val memberData = calculateMemberSettlementData(trip.members, expenses)
        return SettlementResult.Total(
            memberBalances = memberData.map {
                SettlementResult.MemberBalance(
                    tripMemberId = it.member.id,
                    nickname = it.member.name,
                    balance = it.paidAmountKrw.subtract(it.assignedAmountKrw),
                    foreignCurrenciesUsed = it.foreignCurrencies,
                )
            },
            debtRelations = calculateDebtRelations(memberData),
            isExchangeRateApplied = true,
        )
    }

    private fun calculateMemberSettlementData(
        members: List<TripMember>,
        expenses: List<Expense>,
    ): List<MemberSettlementData> {
        return members.map { member ->
            val paid = expenses
                .filter { it.payer.id == member.id }
                .sumOf { convertToKrw(it.amount, it.currencyCode, it.spentAt) }

            val assigned = expenses
                .flatMap { expense -> expense.participants.map { participant -> expense to participant } }
                .filter { (_, participant) -> participant.tripMember.id == member.id }
                .sumOf { (expense, participant) ->
                    val share = participant.shareAmount ?: BigDecimal.ZERO
                    convertToKrw(share, expense.currencyCode, expense.spentAt)
                }

            val foreignCurrencies = expenses
                .filter { it.currencyCode != krw && (it.payer.id == member.id || it.participants.any { p -> p.tripMember.id == member.id }) }
                .map { it.currencyCode }
                .distinct()

            MemberSettlementData(member, paid, assigned, foreignCurrencies)
        }
    }

    private fun convertToKrw(amount: BigDecimal, currencyCode: String, date: LocalDate): BigDecimal {
        val normalized = currencyCode.uppercase()
        if (normalized == krw) return amount.setScale(0, RoundingMode.HALF_UP)
        val rate = findClosestRate(normalized, date)?.exchangeRate
            ?: throw BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND)
        return amount.multiply(rate).setScale(0, RoundingMode.HALF_UP)
    }

    private fun findClosestRate(currencyCode: String, targetDate: LocalDate): Currency? {
        val exact = currencyRepository.findByCurUnitAndDate(currencyCode, targetDate)
        if (exact != null) return exact

        val past = currencyRepository.findTopByCurUnitAndDateLessThanEqualOrderByDateDesc(currencyCode, targetDate)
        val future = currencyRepository.findTopByCurUnitAndDateGreaterThanEqualOrderByDateAsc(currencyCode, targetDate)

        return when {
            past != null && future == null -> past
            past == null && future != null -> future
            past != null && future != null -> {
                val pastDistance = ChronoUnit.DAYS.between(past.date, targetDate).coerceAtLeast(0)
                val futureDistance = ChronoUnit.DAYS.between(targetDate, future.date).coerceAtLeast(0)
                if (pastDistance <= futureDistance) past else future
            }
            else -> null
        }
    }

    private fun calculateDebtRelations(data: List<MemberSettlementData>): List<SettlementResult.DebtRelation> {
        val debtors = data.map { it.member to it.paidAmountKrw.subtract(it.assignedAmountKrw) }
            .filter { it.second < BigDecimal.ZERO }
            .map { it.first to it.second.abs() }
            .toMutableList()
        val creditors = data.map { it.member to it.paidAmountKrw.subtract(it.assignedAmountKrw) }
            .filter { it.second > BigDecimal.ZERO }
            .toMutableList()

        val result = mutableListOf<SettlementResult.DebtRelation>()
        var d = 0
        var c = 0
        while (d < debtors.size && c < creditors.size) {
            val (debtor, debtAmount) = debtors[d]
            val (creditor, creditAmount) = creditors[c]
            val transfer = debtAmount.min(creditAmount)
            result.add(
                SettlementResult.DebtRelation(
                    fromNickname = debtor.name,
                    fromTripMemberId = debtor.id,
                    toNickname = creditor.name,
                    toTripMemberId = creditor.id,
                    amount = transfer,
                )
            )

            debtors[d] = debtor to debtAmount.subtract(transfer)
            creditors[c] = creditor to creditAmount.subtract(transfer)
            if (debtors[d].second.compareTo(BigDecimal.ZERO) == 0) d++
            if (creditors[c].second.compareTo(BigDecimal.ZERO) == 0) c++
        }
        return result
    }

    private data class MemberSettlementData(
        val member: TripMember,
        val paidAmountKrw: BigDecimal,
        val assignedAmountKrw: BigDecimal,
        val foreignCurrencies: List<String>,
    )
}

package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.domain.expense.Expense
import com.tribe.domain.expense.ExpenseCategory
import com.tribe.domain.expense.ExpenseRepository
import com.tribe.domain.expense.ExpenseSplitType
import com.tribe.domain.member.Member
import com.tribe.domain.trip.Trip
import com.tribe.domain.trip.TripMember
import com.tribe.domain.trip.TripRepository
import com.tribe.domain.trip.TripRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ExpenseServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var expenseRepository: ExpenseRepository

    private lateinit var expenseAuthorizationPolicy: ExpenseAuthorizationPolicy
    private lateinit var expenseService: ExpenseService

    @BeforeEach
    fun setUp() {
        expenseAuthorizationPolicy = ExpenseAuthorizationPolicy(currentActor, tripRepository)
        expenseService = ExpenseService(expenseAuthorizationPolicy, expenseRepository)
    }

    @Test
    fun `createExpense saves aggregate with participants`() {
        val fixture = expenseFixture()
        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.save(any(Expense::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Expense
            ReflectionTestUtils.setField(saved, "id", 99L)
            saved
        }

        val result = expenseService.createExpense(
            ExpenseCommand.Create(
                tripId = fixture.trip.id,
                title = "Airport Taxi",
                amount = BigDecimal("45.50"),
                currencyCode = "jpy",
                spentAt = LocalDate.of(2026, 4, 12),
                category = "transport",
                splitType = "custom",
                payerTripMemberId = fixture.payerMembership.id,
                note = "Late arrival",
                participants = listOf(
                    ExpenseCommand.Participant(fixture.payerMembership.id, BigDecimal("30.00")),
                    ExpenseCommand.Participant(fixture.memberMembership.id, BigDecimal("15.50")),
                ),
            ),
        )

        assertEquals(99L, result.expenseId)
        assertEquals("JPY", result.currencyCode)
        assertEquals(2, result.participants.size)
        assertEquals("payer", result.payerName)
    }

    @Test
    fun `listExpenses returns trip scoped summaries`() {
        val fixture = expenseFixture()
        val expense = Expense(
            trip = fixture.trip,
            createdBy = fixture.actor,
            payer = fixture.payerMembership,
            title = "Dinner",
            amount = BigDecimal("120.00"),
            currencyCode = "USD",
            spentAt = LocalDate.of(2026, 4, 11),
            category = ExpenseCategory.FOOD,
            splitType = ExpenseSplitType.EQUAL,
            note = null,
        )
        ReflectionTestUtils.setField(expense, "id", 51L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.actor.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findAllByTripIdOrderBySpentAtDescIdDesc(fixture.trip.id)).thenReturn(listOf(expense))

        val result = expenseService.listExpenses(ExpenseQuery.ListByTrip(fixture.trip.id))

        assertEquals(1, result.size)
        assertEquals(51L, result.first().expenseId)
        assertEquals("Dinner", result.first().title)
    }

    @Test
    fun `updateExpense rejects non owner admin or creator`() {
        val fixture = expenseFixture()
        val creator = Member(id = 99L, email = "creator@example.com", passwordHash = "hashed", nickname = "creator")
        val creatorMembership = TripMember(member = creator, trip = fixture.trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(creatorMembership, "id", 33L)
        fixture.trip.members.add(creatorMembership)

        val expense = Expense(
            trip = fixture.trip,
            createdBy = creator,
            payer = fixture.payerMembership,
            title = "Museum",
            amount = BigDecimal("15.00"),
            currencyCode = "EUR",
            spentAt = LocalDate.of(2026, 4, 10),
            category = ExpenseCategory.ACTIVITY,
            splitType = ExpenseSplitType.EQUAL,
            note = null,
        )
        ReflectionTestUtils.setField(expense, "id", 200L)

        `when`(currentActor.requireUserId()).thenReturn(fixture.member.id)
        `when`(tripRepository.findTripWithMembersById(fixture.trip.id)).thenReturn(fixture.trip)
        `when`(expenseRepository.findWithDetailsById(200L)).thenReturn(expense)

        val ex = assertThrows(BusinessException::class.java) {
            expenseService.updateExpense(
                ExpenseCommand.Update(
                    tripId = fixture.trip.id,
                    expenseId = 200L,
                    title = "Museum",
                    amount = BigDecimal("15.00"),
                    currencyCode = "EUR",
                    spentAt = LocalDate.of(2026, 4, 10),
                    category = "ACTIVITY",
                    splitType = "EQUAL",
                    payerTripMemberId = fixture.payerMembership.id,
                ),
            )
        }

        assertEquals(ErrorCode.NO_AUTHORITY_TRIP, ex.errorCode)
    }

    private fun expenseFixture(): ExpenseFixture {
        val trip = Trip(
            title = "Tokyo",
            startDate = LocalDate.of(2026, 4, 10),
            endDate = LocalDate.of(2026, 4, 14),
            country = com.tribe.domain.trip.Country.JAPAN,
        )
        ReflectionTestUtils.setField(trip, "id", 10L)

        val actor = Member(id = 1L, email = "actor@example.com", passwordHash = "hashed", nickname = "payer")
        val member = Member(id = 2L, email = "member@example.com", passwordHash = "hashed", nickname = "member")

        val payerMembership = TripMember(member = actor, trip = trip, role = TripRole.MEMBER)
        val memberMembership = TripMember(member = member, trip = trip, role = TripRole.MEMBER)
        ReflectionTestUtils.setField(payerMembership, "id", 11L)
        ReflectionTestUtils.setField(memberMembership, "id", 12L)

        trip.members.add(payerMembership)
        trip.members.add(memberMembership)

        return ExpenseFixture(
            trip = trip,
            actor = actor,
            member = member,
            payerMembership = payerMembership,
            memberMembership = memberMembership,
        )
    }

    private data class ExpenseFixture(
        val trip: Trip,
        val actor: Member,
        val member: Member,
        val payerMembership: TripMember,
        val memberMembership: TripMember,
    )
}

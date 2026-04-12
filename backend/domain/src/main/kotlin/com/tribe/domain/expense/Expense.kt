package com.tribe.domain.expense

import com.tribe.domain.member.Member
import com.tribe.domain.trip.Trip
import com.tribe.domain.trip.TripMember
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class Expense(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    val trip: Trip,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    val createdBy: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_trip_member_id", nullable = false)
    var payer: TripMember,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,
    @Column(nullable = false, length = 3)
    var currencyCode: String,
    @Column(nullable = false)
    var spentAt: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: ExpenseCategory,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var splitType: ExpenseSplitType,
    @Column(length = 1000)
    var note: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    val id: Long = 0L

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true)
    val participants: MutableList<ExpenseParticipant> = mutableListOf()

    fun update(
        title: String,
        amount: BigDecimal,
        currencyCode: String,
        spentAt: LocalDate,
        category: ExpenseCategory,
        splitType: ExpenseSplitType,
        payer: TripMember,
        note: String?,
    ) {
        this.title = title
        this.amount = amount
        this.currencyCode = currencyCode
        this.spentAt = spentAt
        this.category = category
        this.splitType = splitType
        this.payer = payer
        this.note = note
    }

    fun replaceParticipants(nextParticipants: List<ExpenseParticipant>) {
        participants.clear()
        participants.addAll(nextParticipants)
    }
}

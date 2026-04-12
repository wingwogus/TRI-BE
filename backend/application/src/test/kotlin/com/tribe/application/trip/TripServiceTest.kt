package com.tribe.application.trip

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.redis.TripInvitationRepository
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.TripAuthorizationPolicy
import com.tribe.domain.community.CommunityPost
import com.tribe.domain.community.CommunityPostRepository
import com.tribe.domain.itinerary.Category
import com.tribe.domain.itinerary.ItineraryItem
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.Country
import com.tribe.domain.trip.Trip
import com.tribe.domain.trip.TripMember
import com.tribe.domain.trip.TripMemberRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class TripServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var tripInvitationRepository: TripInvitationRepository
    @Mock private lateinit var tripAuthorizationPolicy: TripAuthorizationPolicy
    @Mock private lateinit var communityPostRepository: CommunityPostRepository

    private lateinit var tripService: TripService

    @BeforeEach
    fun setUp() {
        tripService = TripService(
            currentActor = currentActor,
            memberRepository = memberRepository,
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository,
            tripInvitationRepository = tripInvitationRepository,
            tripAuthorizationPolicy = tripAuthorizationPolicy,
            communityPostRepository = communityPostRepository,
            appUrl = "http://localhost:3000",
        )
    }

    @Test
    fun `createTrip adds owner membership`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.createTrip(
            TripCommand.Create("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN.code),
        )

        assertEquals("Trip", result.title)
        assertEquals(1, result.members.size)
        assertEquals("tribe", result.members.first().nickname)
    }

    @Test
    fun `getAllTrips maps repository results`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        trip.members.add(TripMember(Member(id = 1L, email = "u@e.com", passwordHash = "p", nickname = "a"), trip, role = TripRole.OWNER))
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripRepository.findTripsByMemberId(1L, PageRequest.of(0, 10))).thenReturn(PageImpl(listOf(trip)))

        val result = tripService.getAllTrips(PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals("Trip", result.content.first().title)
    }

    @Test
    fun `joinTrip rejects kicked member`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        val tripMember = TripMember(member = member, trip = trip, role = TripRole.KICKED)

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripInvitationRepository.getTripId("token")).thenReturn(5L)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)
        `when`(tripMemberRepository.findByTripIdAndMemberId(5L, 1L)).thenReturn(tripMember)

        val ex = assertThrows(BusinessException::class.java) {
            tripService.joinTrip(TripCommand.Join("token"))
        }

        assertEquals(ErrorCode.BANNED_MEMBER, ex.errorCode)
    }

    @Test
    fun `importTrip clones categories and itinerary items`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        val originalTrip = Trip("Original", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val category = Category(originalTrip, 1, "Day1", 1)
        val item = ItineraryItem(category, null, "Dinner", null, 1, "memo")
        category.itineraryItems.add(item)
        originalTrip.categories.add(category)
        val post = CommunityPost(member, originalTrip, "Post", "Content", null)

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(communityPostRepository.findById(5L)).thenReturn(java.util.Optional.of(post))
        `when`(tripRepository.findTripWithFullItineraryById(originalTrip.id)).thenReturn(originalTrip)
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.importTrip(
            TripCommand.Import(5L, "Imported", LocalDate.now(), LocalDate.now().plusDays(2)),
        )

        assertEquals("Imported", result.title)
        assertEquals(1, result.members.size)
    }

    @Test
    fun `addGuest adds guest membership`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)
        `when`(tripAuthorizationPolicy.isTripAdmin(5L)).thenReturn(true)

        val result = tripService.addGuest(TripCommand.AddGuest(5L, "guest"))

        assertEquals(1, result.members.size)
        assertEquals("guest", result.members.first().nickname)
        assertEquals(TripRole.GUEST.name, result.members.first().role)
    }

    @Test
    fun `deleteGuest removes guest membership`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val guest = TripMember(member = null, trip = trip, guestNickname = "guest", role = TripRole.GUEST)
        trip.members.add(guest)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)
        `when`(tripAuthorizationPolicy.isTripAdmin(5L)).thenReturn(true)

        val result = tripService.deleteGuest(TripCommand.DeleteGuest(5L, guest.id))

        assertEquals(0, result.members.size)
    }

    @Test
    fun `leaveTrip marks member as exited`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val actor = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        trip.members.add(TripMember(member = actor, trip = trip, role = TripRole.MEMBER))
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripAuthorizationPolicy.isTripMember(5L)).thenReturn(true)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        tripService.leaveTrip(TripCommand.Leave(5L))

        assertEquals(TripRole.EXITED, trip.members.first().role)
    }

    @Test
    fun `leaveTrip rejects owner`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val actor = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        trip.members.add(TripMember(member = actor, trip = trip, role = TripRole.OWNER))
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripAuthorizationPolicy.isTripMember(5L)).thenReturn(true)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        val ex = assertThrows(BusinessException::class.java) {
            tripService.leaveTrip(TripCommand.Leave(5L))
        }

        assertEquals(ErrorCode.NO_AUTHORITY_TRIP, ex.errorCode)
    }

    @Test
    fun `kickMember marks target as kicked`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val admin = Member(id = 1L, email = "admin@example.com", passwordHash = "hashed", nickname = "admin")
        val member = Member(id = 2L, email = "user@example.com", passwordHash = "hashed", nickname = "member")
        trip.members.add(TripMember(member = admin, trip = trip, role = TripRole.ADMIN))
        trip.members.add(TripMember(member = member, trip = trip, role = TripRole.MEMBER))
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripAuthorizationPolicy.isTripAdmin(5L)).thenReturn(true)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        tripService.kickMember(TripCommand.KickMember(5L, 2L))

        assertEquals(TripRole.KICKED, trip.members.last().role)
    }

    @Test
    fun `assignRole updates target role`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        val owner = Member(id = 1L, email = "owner@example.com", passwordHash = "hashed", nickname = "owner")
        val member = Member(id = 2L, email = "user@example.com", passwordHash = "hashed", nickname = "member")
        trip.members.add(TripMember(member = owner, trip = trip, role = TripRole.OWNER))
        trip.members.add(TripMember(member = member, trip = trip, role = TripRole.MEMBER))
        `when`(tripAuthorizationPolicy.isTripOwner(5L)).thenReturn(true)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        tripService.assignRole(TripCommand.AssignRole(5L, 2L, "admin"))

        assertEquals(TripRole.ADMIN, trip.members.last().role)
    }
}

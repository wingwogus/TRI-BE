package com.tribe.application.trip.core

import com.tribe.application.redis.TripInvitationRepository
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.application.trip.event.TripRealtimeEventPublisher
import com.tribe.application.trip.member.TripMemberIntegrityService
import com.tribe.domain.community.CommunityPost
import com.tribe.domain.community.CommunityPostRepository
import com.tribe.domain.itinerary.item.ItineraryItem
import com.tribe.domain.itinerary.item.ItineraryItemRepository
import com.tribe.domain.itinerary.wishlist.WishlistItemRepository
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import com.tribe.domain.trip.core.Country
import com.tribe.domain.trip.core.Trip
import com.tribe.domain.trip.core.TripRegion
import com.tribe.domain.trip.member.TripMember
import com.tribe.domain.trip.member.TripMemberRepository
import com.tribe.domain.trip.core.TripRepository
import com.tribe.domain.trip.member.TripRole
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.lenient
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TripServiceTest {
    @Mock private lateinit var currentActor: CurrentActor
    @Mock private lateinit var tripAuthorizationPolicy: TripAuthorizationPolicy
    @Mock private lateinit var tripMemberIntegrityService: TripMemberIntegrityService
    @Mock private lateinit var tripRealtimeEventPublisher: TripRealtimeEventPublisher
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var tripRepository: TripRepository
    @Mock private lateinit var tripMemberRepository: TripMemberRepository
    @Mock private lateinit var tripInvitationRepository: TripInvitationRepository
    @Mock private lateinit var communityPostRepository: CommunityPostRepository
    @Mock private lateinit var wishlistItemRepository: WishlistItemRepository
    @Mock private lateinit var itineraryItemRepository: ItineraryItemRepository
    @Mock private lateinit var placeRepository: PlaceRepository

    private lateinit var tripService: TripService

    @BeforeEach
    fun setUp() {
        lenient()
            .`when`(itineraryItemRepository.findByTripIdAndVisitDayGreaterThanOrderByVisitDayAscOrderAsc(anyLong(), anyInt()))
            .thenReturn(emptyList())

        tripService = TripService(
            currentActor = currentActor,
            tripAuthorizationPolicy = tripAuthorizationPolicy,
            tripMemberIntegrityService = tripMemberIntegrityService,
            tripRealtimeEventPublisher = tripRealtimeEventPublisher,
            memberRepository = memberRepository,
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository,
            tripInvitationRepository = tripInvitationRepository,
            communityPostRepository = communityPostRepository,
            wishlistItemRepository = wishlistItemRepository,
            itineraryItemRepository = itineraryItemRepository,
            placeRepository = placeRepository,
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
            TripCommand.Create("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN.code, TripRegion.JP_TOKYO.code),
        )

        assertEquals("Trip", result.title)
        assertEquals(TripRegion.JP_TOKYO.code, result.regionCode)
        assertEquals(1, result.members.size)
        assertEquals("tribe", result.members.first().nickname)
    }

    @Test
    fun `createTrip saves matching region code`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.createTrip(
            TripCommand.Create("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN.code, TripRegion.JP_TOKYO.code),
        )

        assertEquals(TripRegion.JP_TOKYO.code, result.regionCode)
    }

    @Test
    fun `createTrip keeps compatibility when regionCode is missing`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.createTrip(
            TripCommand.Create("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN.code, null),
        )

        assertNull(result.regionCode)
    }

    @Test
    fun `createTripWithItems batches place lookup and attaches itinerary items`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        val breakfast = place(10L, "Breakfast Spot")
        val museum = place(20L, "Museum")
        val itemTime = LocalDateTime.of(2026, 4, 12, 9, 0)

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))
        `when`(placeRepository.findAllById(listOf(20L, 10L))).thenReturn(listOf(museum, breakfast))
        `when`(tripRepository.save(any(Trip::class.java))).thenAnswer { it.arguments[0] as Trip }

        val result = tripService.createTripWithItems(
            TripCommand.CreateWithItems(
                title = "AI Trip",
                startDate = LocalDate.of(2026, 4, 12),
                endDate = LocalDate.of(2026, 4, 13),
                country = Country.JAPAN.code,
                regionCode = TripRegion.JP_TOKYO.code,
                items = listOf(
                    TripCommand.CreateItem(visitDay = 1, order = 2, placeId = 20L, title = "Museum stop", time = itemTime.plusHours(4), memo = "Afternoon"),
                    TripCommand.CreateItem(visitDay = 1, order = 1, placeId = 10L, title = "Breakfast", time = itemTime, memo = "Window seat"),
                    TripCommand.CreateItem(visitDay = 2, order = 1, placeId = null, title = "Free walk", time = null, memo = "  Explore side streets  "),
                ),
            ),
        )

        val tripCaptor = ArgumentCaptor.forClass(Trip::class.java)
        verify(placeRepository, times(1)).findAllById(eq(listOf(20L, 10L)))
        verify(tripRepository).save(tripCaptor.capture())

        val savedTrip = tripCaptor.value

        assertEquals("AI Trip", result.title)
        assertEquals(1, savedTrip.members.size)
        assertEquals(3, savedTrip.itineraryItems.size)
        assertEquals(20L, savedTrip.itineraryItems[0].place?.id)
        assertEquals(2, savedTrip.itineraryItems[0].order)
        assertNull(savedTrip.itineraryItems[0].title)
        assertEquals(10L, savedTrip.itineraryItems[1].place?.id)
        assertEquals(1, savedTrip.itineraryItems[1].order)
        assertEquals(itemTime, savedTrip.itineraryItems[1].time)
        assertEquals("Free walk", savedTrip.itineraryItems[2].title)
        assertEquals("Explore side streets", savedTrip.itineraryItems[2].memo)
        assertNull(savedTrip.itineraryItems[2].place)
    }

    @Test
    fun `createTripWithItems rejects missing place before save`() {
        val breakfast = place(10L, "Breakfast Spot")

        `when`(placeRepository.findAllById(listOf(10L, 20L))).thenReturn(listOf(breakfast))

        val ex = assertThrows(BusinessException::class.java) {
            tripService.createTripWithItems(
                TripCommand.CreateWithItems(
                    title = "AI Trip",
                    startDate = LocalDate.of(2026, 4, 12),
                    endDate = LocalDate.of(2026, 4, 13),
                    country = Country.JAPAN.code,
                    regionCode = TripRegion.JP_TOKYO.code,
                    items = listOf(
                        TripCommand.CreateItem(visitDay = 1, order = 1, placeId = 10L, title = null, time = null, memo = null),
                        TripCommand.CreateItem(visitDay = 1, order = 2, placeId = 20L, title = null, time = null, memo = null),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.PLACE_NOT_FOUND, ex.errorCode)
        verify(tripRepository, never()).save(any(Trip::class.java))
    }

    @Test
    fun `createTripWithItems rejects item without place or title before save`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))

        val ex = assertThrows(BusinessException::class.java) {
            tripService.createTripWithItems(
                TripCommand.CreateWithItems(
                    title = "AI Trip",
                    startDate = LocalDate.of(2026, 4, 12),
                    endDate = LocalDate.of(2026, 4, 13),
                    country = Country.JAPAN.code,
                    regionCode = TripRegion.JP_TOKYO.code,
                    items = listOf(
                        TripCommand.CreateItem(visitDay = 1, order = 1, placeId = null, title = "   ", time = null, memo = null),
                    ),
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.errorCode)
        verify(tripRepository, never()).save(any(Trip::class.java))
    }

    @Test
    fun `createTrip rejects regionCode from another country`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member))

        val ex = assertThrows(BusinessException::class.java) {
            tripService.createTrip(
                TripCommand.Create("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN.code, TripRegion.KR_JEJU.code),
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    @Test
    fun `updateTrip clears region code when blank is requested`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN, TripRegion.JP_TOKYO.code)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        val result = tripService.updateTrip(
            TripCommand.Update(5L, "Trip", trip.startDate, trip.endDate, Country.JAPAN.code, ""),
        )

        assertEquals(null, result.regionCode)
        assertEquals(null, trip.regionCode)
    }

    @Test
    fun `updateTrip returns conflict when date shrink leaves itinerary items out of range`() {
        val trip = Trip(
            "Trip",
            LocalDate.of(2026, 4, 12),
            LocalDate.of(2026, 4, 17),
            Country.JAPAN,
            TripRegion.JP_TOKYO.code,
        )
        val outOfRangeItem = ItineraryItem(trip, 3, null, "Dinner", null, 1, null)

        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)
        `when`(itineraryItemRepository.findByTripIdAndVisitDayGreaterThanOrderByVisitDayAscOrderAsc(5L, 2))
            .thenReturn(listOf(outOfRangeItem))

        val ex = assertThrows(BusinessException::class.java) {
            tripService.updateTrip(
                TripCommand.Update(
                    tripId = 5L,
                    title = "Short trip",
                    startDate = LocalDate.of(2026, 4, 12),
                    endDate = LocalDate.of(2026, 4, 13),
                    country = Country.JAPAN.code,
                    regionCode = TripRegion.JP_TOKYO.code,
                ),
            )
        }

        val detail = ex.detail as Map<*, *>
        assertEquals(ErrorCode.TRIP_DATE_RANGE_REQUIRES_ITEM_DELETION, ex.errorCode)
        assertEquals(1, detail["outOfRangeItemCount"])
        assertEquals(2, detail["newTotalDays"])
        assertEquals(LocalDate.of(2026, 4, 17), trip.endDate)
    }

    @Test
    fun `updateTrip deletes out of range itinerary items when destructive save is confirmed`() {
        val trip = Trip(
            "Trip",
            LocalDate.of(2026, 4, 12),
            LocalDate.of(2026, 4, 17),
            Country.JAPAN,
            TripRegion.JP_TOKYO.code,
        )
        val outOfRangeItem = ItineraryItem(trip, 3, null, "Dinner", null, 1, null)

        `when`(currentActor.requireUserId()).thenReturn(1L)
        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)
        `when`(itineraryItemRepository.findByTripIdAndVisitDayGreaterThanOrderByVisitDayAscOrderAsc(5L, 2))
            .thenReturn(listOf(outOfRangeItem))

        val result = tripService.updateTrip(
            TripCommand.Update(
                tripId = 5L,
                title = "Short trip",
                startDate = LocalDate.of(2026, 4, 12),
                endDate = LocalDate.of(2026, 4, 13),
                country = Country.JAPAN.code,
                regionCode = TripRegion.JP_TOKYO.code,
                deleteOutOfRangeItems = true,
            ),
        )

        assertEquals("Short trip", result.title)
        assertEquals(LocalDate.of(2026, 4, 13), trip.endDate)
        verify(itineraryItemRepository).deleteAll(listOf(outOfRangeItem))
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

        val ex = assertThrows(com.tribe.application.exception.business.BusinessException::class.java) {
            tripService.joinTrip(TripCommand.Join("token"))
        }

        assertEquals(com.tribe.application.exception.ErrorCode.BANNED_MEMBER, ex.errorCode)
    }

    @Test
    fun `importTrip clones categories and itinerary items`() {
        val member = Member(id = 1L, email = "user@example.com", passwordHash = "hashed", nickname = "tribe")
        val originalTrip = Trip("Original", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN, TripRegion.JP_TOKYO.code)
        val item = ItineraryItem(originalTrip, 1, null, "Dinner", null, 1, "memo")
        originalTrip.itineraryItems.add(item)
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
        assertEquals(TripRegion.JP_TOKYO.code, result.regionCode)
        assertEquals(1, result.members.size)
    }

    @Test
    fun `member integrity operations delegate to integrity service`() {
        val delegated = TripResult.TripDetail(
            tripId = 5L,
            title = "Trip",
            startDate = LocalDate.of(2026, 4, 12),
            endDate = LocalDate.of(2026, 4, 13),
            country = "JP",
            regionCode = TripRegion.JP_TOKYO.code,
            members = emptyList(),
        )
        `when`(tripMemberIntegrityService.deleteGuest(TripCommand.DeleteGuest(5L, 10L))).thenReturn(delegated)
        `when`(tripMemberIntegrityService.leaveTrip(TripCommand.Leave(5L))).thenReturn(delegated)
        `when`(tripMemberIntegrityService.kickMember(TripCommand.KickMember(5L, 2L))).thenReturn(delegated)
        `when`(tripMemberIntegrityService.assignRole(TripCommand.AssignRole(5L, 2L, "admin"))).thenReturn(delegated)

        assertEquals(5L, tripService.deleteGuest(TripCommand.DeleteGuest(5L, 10L)).tripId)
        assertEquals(5L, tripService.leaveTrip(TripCommand.Leave(5L)).tripId)
        assertEquals(5L, tripService.kickMember(TripCommand.KickMember(5L, 2L)).tripId)
        assertEquals(5L, tripService.assignRole(TripCommand.AssignRole(5L, 2L, "admin")).tripId)
    }

    @Test
    fun `deleteTrip removes wishlist items before deleting trip`() {
        val trip = Trip("Trip", LocalDate.now(), LocalDate.now().plusDays(1), Country.JAPAN)
        `when`(tripRepository.findById(5L)).thenReturn(java.util.Optional.of(trip))
        `when`(currentActor.requireUserId()).thenReturn(1L)

        tripService.deleteTrip(5L)

        val ordered = inOrder(wishlistItemRepository, tripRepository)
        ordered.verify(wishlistItemRepository).deleteByTripId(5L)
        ordered.verify(tripRepository).delete(trip)
    }

    @Test
    fun `updateTrip preserves existing regionCode when omitted for same country`() {
        val trip = Trip(
            "Trip",
            LocalDate.of(2026, 4, 12),
            LocalDate.of(2026, 4, 13),
            Country.JAPAN,
            regionCode = "JP_TOKYO",
        )

        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        val result = tripService.updateTrip(
            TripCommand.Update(
                tripId = 5L,
                title = "Updated",
                startDate = LocalDate.of(2026, 4, 14),
                endDate = LocalDate.of(2026, 4, 15),
                country = Country.JAPAN.code,
                regionCode = null,
            ),
        )

        assertEquals("JP_TOKYO", result.regionCode)
    }

    @Test
    fun `updateTrip clears regionCode when country changes and regionCode omitted`() {
        val trip = Trip(
            "Trip",
            LocalDate.of(2026, 4, 12),
            LocalDate.of(2026, 4, 13),
            Country.JAPAN,
            regionCode = "JP_TOKYO",
        )

        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        val result = tripService.updateTrip(
            TripCommand.Update(
                tripId = 5L,
                title = "Updated",
                startDate = LocalDate.of(2026, 4, 14),
                endDate = LocalDate.of(2026, 4, 15),
                country = Country.SOUTH_KOREA.code,
                regionCode = null,
            ),
        )

        assertEquals(null, result.regionCode)
    }

    @Test
    fun `updateTrip clears regionCode when same country sends explicit empty regionCode`() {
        val trip = Trip(
            "Trip",
            LocalDate.of(2026, 4, 12),
            LocalDate.of(2026, 4, 13),
            Country.JAPAN,
            regionCode = "JP_TOKYO",
        )

        `when`(tripRepository.findTripWithMembersById(5L)).thenReturn(trip)

        val result = tripService.updateTrip(
            TripCommand.Update(
                tripId = 5L,
                title = "Updated",
                startDate = LocalDate.of(2026, 4, 14),
                endDate = LocalDate.of(2026, 4, 15),
                country = Country.JAPAN.code,
                regionCode = "",
            ),
        )

        assertEquals(null, result.regionCode)
    }

    private fun place(id: Long, name: String): Place {
        val place = Place(
            externalPlaceId = "ext-$id",
            name = name,
            address = "Address $id",
            latitude = BigDecimal("35.0"),
            longitude = BigDecimal("139.0"),
        )
        val idField = Place::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.setLong(place, id)
        return place
    }
}

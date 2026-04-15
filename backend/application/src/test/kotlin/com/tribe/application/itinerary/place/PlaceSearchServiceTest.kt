package com.tribe.application.itinerary.place

import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.place.PlaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PlaceSearchServiceTest {
    @Mock private lateinit var placeSearchGateway: PlaceSearchGateway
    @Mock private lateinit var cacheRepository: PlaceSearchCacheRepository
    @Mock private lateinit var placeCatalogService: PlaceCatalogService
    @Mock private lateinit var placeRepository: PlaceRepository
    @Mock private lateinit var placeViewAssembler: PlaceViewAssembler

    private lateinit var service: PlaceSearchService

    @BeforeEach
    fun setUp() {
        service = PlaceSearchService(
            placeSearchGateway = placeSearchGateway,
            placeSearchCacheRepository = cacheRepository,
            placeCatalogService = placeCatalogService,
            placeRepository = placeRepository,
            placeViewAssembler = placeViewAssembler,
        )
    }

    @Test
    fun `search returns cached results without gateway call`() {
        val cached = listOf(
            PlaceSearchResult(
                externalPlaceId = "place-1",
                placeName = "Tokyo Tower",
                address = "Tokyo",
                latitude = 1.0,
                longitude = 2.0,
            ),
        )
        `when`(cacheRepository.get("tower|ko|country:JP|35.0|139.0|50000")).thenReturn(cached)
        `when`(placeCatalogService.mergeWithCanonical(cached)).thenReturn(cached)

        val result = service.search("tower", "ko", "JP", 35.0, 139.0, 500000, "country:JP")

        assertEquals(1, result.size)
        verifyNoInteractions(placeSearchGateway)
    }

    @Test
    fun `search clamps radius before gateway call`() {
        val expectedContext = PlaceSearchContext(
            regionCode = "JP",
            latitude = 35.0,
            longitude = 139.0,
            radiusMeters = 50_000,
            regionContextKey = "country:JP",
        )
        `when`(cacheRepository.get("tower|ko|country:JP|35.0|139.0|50000")).thenReturn(null)
        `when`(placeSearchGateway.search("tower", "ko", expectedContext)).thenReturn(emptyList())
        `when`(placeCatalogService.mergeWithCanonical(emptyList())).thenReturn(emptyList())

        val result = service.search("tower", "ko", "JP", 35.0, 139.0, 500000, "country:JP")

        assertEquals(0, result.size)
        verify(placeSearchGateway).search("tower", "ko", expectedContext)
    }

    @Test
    fun `search defaults radius and normalizes region when coordinates exist`() {
        val expectedContext = PlaceSearchContext(
            regionCode = "JP",
            latitude = 35.0,
            longitude = 139.0,
            radiusMeters = 50_000,
            regionContextKey = "country:JP",
        )
        `when`(cacheRepository.get("tower|ko|country:JP|35.0|139.0|50000")).thenReturn(null)
        `when`(placeSearchGateway.search("tower", "ko", expectedContext)).thenReturn(emptyList())
        `when`(placeCatalogService.mergeWithCanonical(emptyList())).thenReturn(emptyList())

        service.search("tower", "ko", "jp", 35.0, 139.0, null, "country:JP")

        verify(placeSearchGateway).search("tower", "ko", expectedContext)
    }

    @Test
    fun `getPlaceDetail enriches and assembles view`() {
        val place = Place(
            externalPlaceId = "place-1",
            name = "Tokyo Tower",
            address = "Tokyo",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )
        ReflectionTestUtils.setField(place, "id", 10L)
        val view = PlaceDetailView(
            placeId = 10L,
            externalPlaceId = "place-1",
            placeName = "Tokyo Tower",
            address = "Tokyo",
            latitude = 1.0,
            longitude = 10.0,
            placeTypeSummary = null,
            normalizedCategoryKey = null,
            photoHint = null,
            placeDetailSummary = null,
            formattedPhoneNumber = null,
            internationalPhoneNumber = null,
            websiteUri = null,
            googleMapsUri = null,
            regularOpeningHoursJson = null,
            currentOpeningHoursJson = null,
        )
        `when`(placeRepository.findById(10L)).thenReturn(Optional.of(place))
        `when`(placeCatalogService.enrichDetailsIfNeeded(place, "ko")).thenReturn(place)
        `when`(placeViewAssembler.toDetailView(place)).thenReturn(view)

        val result = service.getPlaceDetail(10L, "ko")

        assertEquals(10L, result.placeId)
    }
}

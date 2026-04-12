package com.tribe.domain.itinerary

import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByExternalPlaceId(externalPlaceId: String): Place?
}

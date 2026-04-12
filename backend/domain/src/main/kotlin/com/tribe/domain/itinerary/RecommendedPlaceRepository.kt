package com.tribe.domain.itinerary

import org.springframework.data.jpa.repository.JpaRepository

interface RecommendedPlaceRepository : JpaRepository<RecommendedPlace, Long>

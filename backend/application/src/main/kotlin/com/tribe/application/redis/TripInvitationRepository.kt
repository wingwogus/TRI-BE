package com.tribe.application.redis

import java.time.Duration

interface TripInvitationRepository {
    fun save(token: String, tripId: Long, ttl: Duration)
    fun getTripId(token: String): Long?
}

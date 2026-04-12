package com.tribe.application.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class TripInvitationRedisRepository(
    private val redis: StringRedisTemplate,
) : TripInvitationRepository {
    companion object {
        private const val PREFIX = "invite:"
    }

    override fun save(token: String, tripId: Long, ttl: Duration) {
        redis.opsForValue().set(PREFIX + token, tripId.toString(), ttl)
    }

    override fun getTripId(token: String): Long? {
        return redis.opsForValue().get(PREFIX + token)?.toLongOrNull()
    }
}

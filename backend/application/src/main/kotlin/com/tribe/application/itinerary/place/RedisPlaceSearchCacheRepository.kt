package com.tribe.application.itinerary.place

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisPlaceSearchCacheRepository(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : PlaceSearchCacheRepository {
    companion object {
        private const val PREFIX = "place-search:"
    }

    override fun get(key: String): List<PlaceSearchResult>? {
        val payload = redis.opsForValue().get(PREFIX + key) ?: return null
        return objectMapper.readValue(payload, object : TypeReference<List<PlaceSearchResult>>() {})
    }

    override fun put(key: String, value: List<PlaceSearchResult>, ttl: Duration) {
        redis.opsForValue().set(PREFIX + key, objectMapper.writeValueAsString(value), ttl)
    }
}

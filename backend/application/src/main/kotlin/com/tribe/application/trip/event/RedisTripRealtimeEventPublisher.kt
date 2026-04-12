package com.tribe.application.trip.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["trip.realtime.enabled"], havingValue = "true")
class RedisTripRealtimeEventPublisher(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : TripRealtimeEventPublisher {

    companion object {
        const val CHANNEL = "tribe:trip-events"
    }

    override fun publish(event: TripRealtimeEvent) {
        redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event))
    }
}

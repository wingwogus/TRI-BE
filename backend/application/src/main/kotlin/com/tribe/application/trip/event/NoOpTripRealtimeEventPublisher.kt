package com.tribe.application.trip.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["trip.realtime.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpTripRealtimeEventPublisher : TripRealtimeEventPublisher {
    override fun publish(event: TripRealtimeEvent) {
        // local/test can disable trip realtime transport while preserving mutation behavior
    }
}

package com.tribe.application.trip.event

interface TripRealtimeEventPublisher {
    fun publish(event: TripRealtimeEvent)
}

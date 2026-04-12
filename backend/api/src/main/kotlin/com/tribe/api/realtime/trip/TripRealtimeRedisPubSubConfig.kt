package com.tribe.api.realtime.trip

import com.tribe.application.trip.event.RedisTripRealtimeEventPublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
@ConditionalOnProperty(name = ["trip.realtime.enabled"], havingValue = "true")
class TripRealtimeRedisPubSubConfig {
    @Bean
    fun tripRealtimeMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        subscriber: TripRealtimeSubscriber,
    ): RedisMessageListenerContainer {
        return RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            addMessageListener(subscriber, ChannelTopic(RedisTripRealtimeEventPublisher.CHANNEL))
        }
    }
}

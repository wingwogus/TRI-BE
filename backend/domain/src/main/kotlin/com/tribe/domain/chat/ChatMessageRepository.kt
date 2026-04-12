package com.tribe.domain.chat

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    @Query(
        """
        select cm from ChatMessage cm
        join fetch cm.sender s
        left join fetch s.member m
        where cm.trip.id = :tripId
          and (:cursorCreatedAt is null or cm.createdAt < :cursorCreatedAt or (cm.createdAt = :cursorCreatedAt and cm.id < :cursorId))
        order by cm.createdAt desc, cm.id desc
        """
    )
    fun findHistoryPage(
        @Param("tripId") tripId: Long,
        @Param("cursorCreatedAt") cursorCreatedAt: java.time.LocalDateTime?,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable,
    ): List<ChatMessage>
}

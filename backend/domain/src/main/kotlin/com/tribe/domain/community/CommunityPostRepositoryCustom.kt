package com.tribe.domain.community

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommunityPostRepositoryCustom {
    fun searchPost(condition: PostSearchCondition, pageable: Pageable): Page<CommunityPost>
}

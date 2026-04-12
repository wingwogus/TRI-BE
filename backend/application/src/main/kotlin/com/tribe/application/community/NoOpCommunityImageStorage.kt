package com.tribe.application.community

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
@ConditionalOnProperty(name = ["tribe.community.image.enabled"], havingValue = "false")
class NoOpCommunityImageStorage : CommunityImageStorage {
    override fun upload(file: MultipartFile, folder: String): String {
        return ""
    }

    override fun delete(imageUrl: String) {
        // no-op fallback for contexts that do not wire the real media adapter
    }
}

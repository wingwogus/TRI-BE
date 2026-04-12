package com.tribe.application.community

import org.springframework.web.multipart.MultipartFile

interface CommunityImageStorage {
    fun upload(file: MultipartFile, folder: String): String
    fun delete(imageUrl: String)
}

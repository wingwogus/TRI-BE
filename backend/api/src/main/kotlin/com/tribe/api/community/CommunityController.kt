package com.tribe.api.community

import com.tribe.api.common.ApiResponse
import com.tribe.application.community.CommunityQuery
import com.tribe.application.community.CommunityService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Validated
@RestController
@RequestMapping("/api/v1/community/posts")
class CommunityController(
    private val communityService: CommunityService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPost(
        @Valid @RequestPart("request") request: CommunityRequests.CreatePostRequest,
        @RequestPart(value = "image", required = false) imageFile: MultipartFile?,
    ): ResponseEntity<ApiResponse<CommunityResponses.PostDetailResponse>> {
        val post = communityService.createPost(
            CommunityQuery.CreatePost(request.tripId, request.title, request.content),
            imageFile,
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(CommunityResponses.PostDetailResponse.from(post)))
    }

    @GetMapping
    fun listPosts(
        @Valid @ModelAttribute request: CommunityRequests.ListPostsRequest
    ): ResponseEntity<ApiResponse<CommunityResponses.PostListResponse>> {
        val posts = communityService.listPosts(
            CommunityQuery.ListPosts(
                page = request.page,
                size = request.size,
                country = request.country,
                authorId = request.authorId,
            )
        )

        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostListResponse.from(posts)))
    }

    @GetMapping("/{postId}")
    fun getPostDetail(
        @PathVariable postId: Long
    ): ResponseEntity<ApiResponse<CommunityResponses.PostDetailResponse>> {
        val post = communityService.getPostDetail(CommunityQuery.GetPostDetail(postId))
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostDetailResponse.from(post)))
    }

    @PatchMapping("/{postId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updatePost(
        @PathVariable postId: Long,
        @Valid @RequestPart("request") request: CommunityRequests.UpdatePostRequest,
        @RequestPart(value = "image", required = false) imageFile: MultipartFile?,
    ): ResponseEntity<ApiResponse<CommunityResponses.PostDetailResponse>> {
        val post = communityService.updatePost(
            CommunityQuery.UpdatePost(postId, request.title, request.content),
            imageFile,
        )
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostDetailResponse.from(post)))
    }

    @DeleteMapping("/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        communityService.deletePost(CommunityQuery.DeletePost(postId))
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }
}

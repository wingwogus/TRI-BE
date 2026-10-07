package com.tribe.api.trip.ai

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.trip.ai.GeminiGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientRequestException

@Component
@ConditionalOnProperty(name = ["trip.review.ai.provider"], havingValue = "gemini", matchIfMissing = true)
class GeminiWebClientGateway(
    webClientBuilder: WebClient.Builder,
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.api.url}") private val apiUrl: String,
) : GeminiGateway {
    private val webClient = webClientBuilder.build()

    override fun generate(prompt: String): String? {
        return execute(buildRequestBody(prompt))
    }

    override fun generateJson(prompt: String, schema: Map<String, Any>): String? {
        return execute(buildJsonRequestBody(prompt, schema))
    }

    private fun execute(requestBody: Map<String, Any>): String? {
        val response = webClient.post()
            .uri("$apiUrl?key=$apiKey")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map::class.java)
            .onErrorMap(WebClientException::class.java) { exception ->
                BusinessException(
                    ErrorCode.AI_FEEDBACK_ERROR,
                    customMessage = if (exception is WebClientRequestException) {
                        "AI 서버에 연결할 수 없습니다. AI 서버 실행 상태를 확인해주세요."
                    } else {
                        "AI 서버가 요청을 처리하지 못했습니다. 모델 및 API 설정을 확인해주세요."
                    },
                )
            }
            .block()
            ?: return null

        return extractResponseText(response)
    }

    internal fun buildRequestBody(prompt: String): Map<String, Any> {
        return mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )
    }

    internal fun buildJsonRequestBody(prompt: String, schema: Map<String, Any>): Map<String, Any> {
        return buildRequestBody(prompt) + mapOf(
            "generationConfig" to mapOf(
                "temperature" to 0.4,
                "maxOutputTokens" to 8192,
                "responseFormat" to mapOf(
                    "text" to mapOf(
                        "mimeType" to "application/json",
                        "schema" to schema,
                    ),
                ),
            ),
        )
    }

    internal fun extractResponseText(response: Map<*, *>): String? {
        val candidates = response["candidates"] as? List<*> ?: return null
        val first = candidates.firstOrNull() as? Map<*, *> ?: return null
        val content = first["content"] as? Map<*, *> ?: return null
        val parts = content["parts"] as? List<*> ?: return null
        val firstPart = parts.firstOrNull() as? Map<*, *> ?: return null
        return firstPart["text"] as? String
    }
}

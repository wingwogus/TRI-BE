package com.tribe.api.trip.ai

import com.tribe.application.trip.ai.GeminiGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

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

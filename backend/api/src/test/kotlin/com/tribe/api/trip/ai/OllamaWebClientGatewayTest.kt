package com.tribe.api.trip.ai

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import java.net.ConnectException

class OllamaWebClientGatewayTest {
    private val gateway = OllamaWebClientGateway(
        webClientBuilder = WebClient.builder(),
        apiUrl = "http://localhost:11434/api/generate",
        model = "gemma3",
    )

    @Test
    fun `buildRequestBody uses ollama generate payload`() {
        val body = gateway.buildRequestBody("hello")

        assertEquals("gemma3", body["model"])
        assertEquals("hello", body["prompt"])
        assertEquals(false, body["stream"])
    }

    @Test
    fun `buildJsonRequestBody sends schema through ollama format`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf("title" to mapOf("type" to "string")),
        )

        val body = gateway.buildJsonRequestBody("generate trip", schema)

        assertEquals(schema, body["format"])
        assertEquals(mapOf("temperature" to 0), body["options"])
        assertEquals(false, body["stream"])
    }

    @Test
    fun `extractResponseText returns ollama response field`() {
        val response = mapOf(
            "response" to "ollama-output",
            "done" to true,
        )

        assertEquals("ollama-output", gateway.extractResponseText(response))
    }

    @Test
    fun `extractResponseText returns null when response is missing`() {
        assertNull(gateway.extractResponseText(emptyMap<String, Any>()))
    }

    @Test
    fun `provider connection failures use AI error without exposing request details`() {
        val failingGateway = gatewayWithExchange { request ->
            Mono.error(
                WebClientRequestException(
                    ConnectException("Connection refused"), request.method(), request.url(), request.headers(),
                ),
            )
        }

        for (generate in listOf<() -> String?>(
            { failingGateway.generate("hello") },
            { failingGateway.generateJson("hello", mapOf("type" to "object")) },
        )) {
            val exception = assertThrows(BusinessException::class.java) { generate() }
            assertEquals(ErrorCode.AI_FEEDBACK_ERROR, exception.errorCode)
            assertEquals("AI 서버에 연결할 수 없습니다. AI 서버 실행 상태를 확인해주세요.", exception.message)
            assertNull(exception.detail)
            assertFalse(exception.message.orEmpty().contains("test-secret-key"))
            assertNull(exception.cause)
        }
    }

    @Test
    fun `provider HTTP failures use AI error`() {
        for (status in listOf(HttpStatus.UNAUTHORIZED, HttpStatus.SERVICE_UNAVAILABLE)) {
            val failingGateway = gatewayWithExchange {
                Mono.just(ClientResponse.create(status).body("private upstream error").build())
            }

            for (generate in listOf<() -> String?>(
                { failingGateway.generate("hello") },
                { failingGateway.generateJson("hello", mapOf("type" to "object")) },
            )) {
                val exception = assertThrows(BusinessException::class.java) { generate() }
                assertEquals(ErrorCode.AI_FEEDBACK_ERROR, exception.errorCode)
                assertEquals("AI 서버가 요청을 처리하지 못했습니다. 모델 및 API 설정을 확인해주세요.", exception.message)
                assertNull(exception.detail)
                assertFalse(exception.message.orEmpty().contains("private upstream error"))
                assertFalse(exception.message.orEmpty().contains("test-secret-key"))
            }
        }
    }

    @Test
    fun `successful provider responses still return generated text`() {
        val successfulGateway = gatewayWithExchange {
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""{"response":"generated-output"}""")
                    .build(),
            )
        }

        assertEquals("generated-output", successfulGateway.generate("hello"))
        assertEquals("generated-output", successfulGateway.generateJson("hello", mapOf("type" to "object")))
    }

    private fun gatewayWithExchange(exchangeFunction: ExchangeFunction) =
        OllamaWebClientGateway(
            webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction),
            apiUrl = "http://localhost:11434/api/generate",
            model = "gemma3",
        )
}

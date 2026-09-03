package com.tribe.api.trip.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

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
}

package com.tribe.application.trip.ai

interface GeminiGateway {
    fun generate(prompt: String): String?

    fun generateJson(prompt: String, schema: Map<String, Any>): String?
}

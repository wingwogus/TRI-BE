package com.tribe.application.trip

interface GeminiGateway {
    fun generate(prompt: String): String?
}

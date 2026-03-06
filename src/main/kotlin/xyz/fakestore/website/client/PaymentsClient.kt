package xyz.fakestore.website.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class PaymentsClient(@Value("\${services.payments.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getMe(token: String): String? = runCatching {
        val traceId = MDC.get("traceId")
        webClient.get()
            .uri("/api/payments/me")
            .header("Authorization", "Bearer $token")
            .let { if (traceId != null) it.header("X-Trace-Id", traceId) else it }
            .retrieve()
            .bodyToMono<String>()
            .block()
    }.getOrNull()
}

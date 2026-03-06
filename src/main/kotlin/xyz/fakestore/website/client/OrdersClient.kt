package xyz.fakestore.website.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

data class OrdersMeResponse(val message: String, val userId: UUID)

@Component
class OrdersClient(@Value("\${services.orders.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getMe(token: String): OrdersMeResponse? = runCatching {
        val traceId = MDC.get("traceId")
        webClient.get()
            .uri("/api/orders/me")
            .header("Authorization", "Bearer $token")
            .let { if (traceId != null) it.header("X-Trace-Id", traceId) else it }
            .retrieve()
            .bodyToMono<OrdersMeResponse>()
            .block()
    }.getOrNull()
}

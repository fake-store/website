package xyz.fakestore.website.client

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigDecimal
import java.util.UUID

data class CartItemDto(
    val productId: UUID,
    val title: String,
    val price: BigDecimal,
    val quantity: Int
)

data class CartItemView(
    val productId: UUID,
    val title: String,
    val price: BigDecimal,
    val quantity: Int
)

@Component
class CartClient(@Value("\${services.orders.url}") baseUrl: String) {

    private val log = LoggerFactory.getLogger(CartClient::class.java)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getItems(token: String): List<CartItemView> = runCatching {
        webClient.get()
            .uri("/api/cart")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<List<CartItemView>>()
            .block() ?: emptyList()
    }.onFailure { log.error("CartClient.getItems failed", it) }
     .getOrDefault(emptyList())

    fun addItem(token: String, item: CartItemDto): Boolean = runCatching {
        webClient.post()
            .uri("/api/cart/items")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(item)
            .retrieve()
            .toBodilessEntity()
            .block()
        true
    }.onFailure { log.error("CartClient.addItem failed", it) }
     .getOrDefault(false)

    fun removeItem(token: String, productId: UUID) = runCatching {
        webClient.delete()
            .uri("/api/cart/items/$productId")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .toBodilessEntity()
            .block()
    }.onFailure { log.error("CartClient.removeItem failed", it) }
     .getOrNull()

    fun clearCart(token: String) = runCatching {
        webClient.delete()
            .uri("/api/cart")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .toBodilessEntity()
            .block()
    }.onFailure { log.error("CartClient.clearCart failed", it) }
     .getOrNull()

    fun mergeItems(token: String, items: List<CartItemDto>) = runCatching {
        webClient.post()
            .uri("/api/cart/merge")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(items)
            .retrieve()
            .toBodilessEntity()
            .block()
    }.onFailure { log.error("CartClient.mergeItems failed", it) }
     .getOrNull()
}

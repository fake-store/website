package xyz.fakestore.website.client

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PlaceOrderItem(val productId: UUID, val title: String, val price: BigDecimal, val quantity: Int)
data class PlaceOrderRequest(
    val userId: UUID,
    val userPaymentMethodId: UUID,
    val shippingAddressId: UUID,
    val items: List<PlaceOrderItem>
)
data class PlaceOrderResponse(val requestId: UUID, val orderId: UUID, val status: String)

data class OrderListItem(
    val id: UUID,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant
)

data class OrderDetailItem(val productId: UUID, val title: String, val price: BigDecimal, val quantity: Int)
data class OrderDetail(
    val id: UUID,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant,
    val paymentMethodId: UUID,
    val shippingAddressId: UUID,
    val items: List<OrderDetailItem>
)

@Component
class OrdersClient(@Value("\${services.orders.url}") baseUrl: String) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getMe(token: String): List<OrderListItem> = runCatching {
        webClient.get()
            .uri("/api/orders/me")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<List<OrderListItem>>()
            .block() ?: emptyList()
    }.onFailure { log.warn("OrdersClient.getMe failed", it) }.getOrDefault(emptyList())

    fun placeOrder(token: String, request: PlaceOrderRequest): PlaceOrderResponse? = runCatching {
        webClient.post()
            .uri("/api/orders/request-payment")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(request)
            .retrieve()
            .bodyToMono<PlaceOrderResponse>()
            .block()
    }.onFailure { log.warn("OrdersClient.placeOrder failed", it) }.getOrNull()

    fun getOrderDetail(token: String, orderId: UUID): OrderDetail? = runCatching {
        webClient.get()
            .uri("/api/orders/$orderId")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<OrderDetail>()
            .block()
    }.onFailure { log.warn("OrdersClient.getOrderDetail failed", it) }.getOrNull()
}

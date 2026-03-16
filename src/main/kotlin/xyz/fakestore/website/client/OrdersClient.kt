package xyz.fakestore.website.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import xyz.fakestore.website.web.CartItem
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderItem(
    val id: UUID,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant
)

data class PlaceOrderItem(
    val productId: UUID,
    val title: String,
    val price: BigDecimal,
    val quantity: Int
)

data class PlaceOrderRequest(
    val userId: UUID,
    val userPaymentMethodId: UUID,
    val shippingAddressId: UUID,
    val items: List<PlaceOrderItem>
)

data class PlaceOrderResponse(val requestId: UUID, val orderId: UUID, val status: String)

data class OrderStatusResponse(val orderId: UUID, val status: String)

@Component
class OrdersClient(@Value("\${services.orders.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getOrders(token: String): List<OrderItem>? = runCatching {
        val traceId = MDC.get("traceId")
        webClient.get()
            .uri("/api/orders/me")
            .header("Authorization", "Bearer $token")
            .let { if (traceId != null) it.header("X-Trace-Id", traceId) else it }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<OrderItem>>() {})
            .block()
    }.getOrNull()

    fun getOrder(token: String, orderId: UUID): OrderItem? = runCatching {
        val traceId = MDC.get("traceId")
        webClient.get()
            .uri("/api/orders/$orderId")
            .header("Authorization", "Bearer $token")
            .let { if (traceId != null) it.header("X-Trace-Id", traceId) else it }
            .retrieve()
            .bodyToMono(OrderItem::class.java)
            .block()
    }.getOrNull()

    fun getOrderStatus(token: String, orderId: UUID): OrderStatusResponse? = runCatching {
        val traceId = MDC.get("traceId")
        webClient.get()
            .uri("/api/orders/$orderId/status")
            .header("Authorization", "Bearer $token")
            .let { if (traceId != null) it.header("X-Trace-Id", traceId) else it }
            .retrieve()
            .bodyToMono(OrderStatusResponse::class.java)
            .block()
    }.getOrNull()

    fun placeOrder(
        token: String,
        userId: UUID,
        paymentMethodId: UUID,
        shippingAddressId: UUID,
        cart: List<CartItem>
    ): PlaceOrderResponse? = runCatching {
        val traceId = MDC.get("traceId")
        val request = PlaceOrderRequest(
            userId = userId,
            userPaymentMethodId = paymentMethodId,
            shippingAddressId = shippingAddressId,
            items = cart.map { PlaceOrderItem(it.productId, it.title, it.price, it.quantity) }
        )
        webClient.post()
            .uri("/api/orders/request-payment")
            .header("Authorization", "Bearer $token")
            .let { if (traceId != null) it.header("X-Trace-Id", traceId) else it }
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PlaceOrderResponse::class.java)
            .block()
    }.getOrNull()
}

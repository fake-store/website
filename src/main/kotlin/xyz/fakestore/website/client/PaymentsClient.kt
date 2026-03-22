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

data class PaymentMethod(
    val userPaymentMethodId: UUID,
    val userId: UUID,
    val type: String,
    val label: String,
    val isDefault: Boolean,
    val active: Boolean
)

data class AddPaymentMethodRequest(val type: String, val label: String)
data class UpdatePaymentMethodRequest(val label: String, val isDefault: Boolean)
data class PaymentHistoryItem(
    val userPaymentRequestId: UUID,
    val orderId: UUID,
    val userPaymentMethodId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant
)

@Component
class PaymentsClient(@Value("\${services.payments.url}") baseUrl: String) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getMethods(token: String): List<PaymentMethod>? = runCatching {
        webClient.get()
            .uri("/api/payments/methods")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<List<PaymentMethod>>()
            .block()
    }.onFailure { log.warn("PaymentsClient.getMethods failed", it) }.getOrNull()

    fun addMethod(token: String, type: String, label: String): PaymentMethod? = runCatching {
        webClient.post()
            .uri("/api/payments/methods")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(AddPaymentMethodRequest(type, label))
            .retrieve()
            .bodyToMono<PaymentMethod>()
            .block()
    }.onFailure { log.warn("PaymentsClient.addMethod failed", it) }.getOrNull()

    fun updateMethod(token: String, methodId: UUID, label: String, isDefault: Boolean): PaymentMethod? = runCatching {
        webClient.post()
            .uri("/api/payments/methods/$methodId/update")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(UpdatePaymentMethodRequest(label, isDefault))
            .retrieve()
            .bodyToMono<PaymentMethod>()
            .block()
    }.onFailure { log.warn("PaymentsClient.updateMethod failed", it) }.getOrNull()

    fun deleteMethod(token: String, methodId: UUID): Boolean = runCatching {
        webClient.post()
            .uri("/api/payments/methods/$methodId/delete")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .toBodilessEntity()
            .block()
        true
    }.onFailure { log.warn("PaymentsClient.deleteMethod failed", it) }.getOrDefault(false)

    fun getHistory(token: String): List<PaymentHistoryItem>? = runCatching {
        webClient.get()
            .uri("/api/payments/history")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<List<PaymentHistoryItem>>()
            .block()
    }.onFailure { log.warn("PaymentsClient.getHistory failed", it) }.getOrNull()
}

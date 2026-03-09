package xyz.fakestore.website.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
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

@Component
class PaymentsClient(@Value("\${services.payments.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getMethods(token: String): List<PaymentMethod>? = runCatching {
        webClient.get()
            .uri("/api/payments/methods")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<List<PaymentMethod>>()
            .block()
    }.getOrNull()

    fun addMethod(token: String, type: String, label: String): PaymentMethod? = runCatching {
        webClient.post()
            .uri("/api/payments/methods")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(AddPaymentMethodRequest(type, label))
            .retrieve()
            .bodyToMono<PaymentMethod>()
            .block()
    }.getOrNull()

    fun updateMethod(token: String, methodId: UUID, label: String, isDefault: Boolean): PaymentMethod? = runCatching {
        webClient.post()
            .uri("/api/payments/methods/$methodId/update")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(UpdatePaymentMethodRequest(label, isDefault))
            .retrieve()
            .bodyToMono<PaymentMethod>()
            .block()
    }.getOrNull()

    fun deleteMethod(token: String, methodId: UUID): Boolean = runCatching {
        webClient.post()
            .uri("/api/payments/methods/$methodId/delete")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .toBodilessEntity()
            .block()
        true
    }.getOrDefault(false)
}

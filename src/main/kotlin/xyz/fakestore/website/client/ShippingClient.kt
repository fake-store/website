package xyz.fakestore.website.client

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

data class ShippingAddress(
    val id: UUID,
    val userId: UUID,
    val label: String,
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class CreateAddressRequest(
    val label: String,
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class UpdateAddressRequest(
    val label: String?,
    val street: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
)

@Component
class ShippingClient(@Value("\${services.shipping.url}") baseUrl: String) {

    private val log = LoggerFactory.getLogger(ShippingClient::class.java)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun isAvailable(): Boolean = runCatching {
        webClient.get().uri("/health").retrieve().toBodilessEntity().block()
        true
    }.onFailure { log.warn("Shipping service unavailable (health check): {}", it.message) }.getOrDefault(false)

    fun getAddresses(token: String): List<ShippingAddress>? = runCatching {
        webClient.get()
            .uri("/api/shipping/addresses")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<List<ShippingAddress>>()
            .block()
    }.onFailure { log.warn("Shipping service unavailable (getAddresses): {}", it.message) }.getOrNull()

    fun addAddress(token: String, req: CreateAddressRequest): ShippingAddress? = runCatching {
        webClient.post()
            .uri("/api/shipping/addresses")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(req)
            .retrieve()
            .bodyToMono<ShippingAddress>()
            .block()
    }.onFailure { log.warn("Shipping service unavailable (addAddress): {}", it.message) }.getOrNull()

    fun updateAddress(token: String, id: UUID, req: UpdateAddressRequest): ShippingAddress? = runCatching {
        webClient.put()
            .uri("/api/shipping/addresses/$id")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(req)
            .retrieve()
            .bodyToMono<ShippingAddress>()
            .block()
    }.onFailure { log.warn("Shipping service unavailable (updateAddress): {}", it.message) }.getOrNull()

    fun deleteAddress(token: String, id: UUID): Boolean = runCatching {
        webClient.delete()
            .uri("/api/shipping/addresses/$id")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .toBodilessEntity()
            .block()
        true
    }.onFailure { log.warn("Shipping service unavailable (deleteAddress): {}", it.message) }.getOrDefault(false)
}

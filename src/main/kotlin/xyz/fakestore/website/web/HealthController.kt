package xyz.fakestore.website.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(
    @Value("\${services.users.url}") val usersUrl: String,
    @Value("\${services.payments.url}") val paymentsUrl: String,
    @Value("\${services.orders.url}") val ordersUrl: String,
    @Value("\${services.shipping.url}") val shippingUrl: String,
    @Value("\${services.catalog.url}") val catalogUrl: String,
    @Value("\${spring.profiles.active:none}") val activeProfile: String,
    @Value("\${server.port}") val serverPort: String,
) {
    @GetMapping("/health")
    fun health() = mapOf(
        "status" to "healthy",
        "profile" to activeProfile,
        "port" to serverPort,
        "services" to mapOf(
            "users" to usersUrl,
            "payments" to paymentsUrl,
            "orders" to ordersUrl,
            "shipping" to shippingUrl,
            "catalog" to catalogUrl,
        )
    )
}

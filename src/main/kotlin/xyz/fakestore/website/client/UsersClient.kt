package xyz.fakestore.website.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class LoginResponse(val token: String, val userId: UUID, val username: String, val email: String)
data class UserResponse(val userId: UUID, val username: String, val email: String)

@Component
class UsersClient(@Value("\${services.users.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun login(email: String, password: String): LoginResponse? =
        webClient.post()
            .uri("/api/users/login")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(LoginRequest(email, password))
            .retrieve()
            .bodyToMono<LoginResponse>()
            .block()

    fun register(username: String, email: String, password: String): LoginResponse? =
        webClient.post()
            .uri("/api/users/register")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(RegisterRequest(username, email, password))
            .retrieve()
            .bodyToMono<LoginResponse>()
            .block()

    fun getMe(token: String): UserResponse? =
        webClient.get()
            .uri("/api/users/me")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .bodyToMono<UserResponse>()
            .block()
}

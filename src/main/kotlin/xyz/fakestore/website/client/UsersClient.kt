package xyz.fakestore.website.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.UUID

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)
data class LoginResponse(val token: String, val refreshToken: String, val userId: UUID, val username: String, val email: String)
data class UserResponse(val userId: UUID, val username: String, val email: String)
data class UpdateEmailRequest(val email: String)
data class UpdateUsernameRequest(val username: String)

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
            .onErrorResume(WebClientResponseException::class.java) { Mono.empty() }
            .block()

    fun deleteMe(token: String) {
        webClient.delete()
            .uri("/api/users/me")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    fun refresh(refreshToken: String): LoginResponse? =
        webClient.post()
            .uri("/api/users/refresh")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(RefreshRequest(refreshToken))
            .retrieve()
            .bodyToMono<LoginResponse>()
            .block()

    fun updateEmail(token: String, email: String): UserResponse? =
        webClient.patch()
            .uri("/api/users/me/email")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(UpdateEmailRequest(email))
            .retrieve()
            .bodyToMono<UserResponse>()
            .onErrorResume(WebClientResponseException::class.java) { Mono.empty() }
            .block()

    fun updateUsername(token: String, username: String): UserResponse? =
        webClient.patch()
            .uri("/api/users/me/username")
            .header("Authorization", "Bearer $token")
            .let { spec -> MDC.get("traceId")?.let { spec.header("X-Trace-Id", it) } ?: spec }
            .bodyValue(UpdateUsernameRequest(username))
            .retrieve()
            .bodyToMono<UserResponse>()
            .onErrorResume(WebClientResponseException::class.java) { Mono.empty() }
            .block()
}

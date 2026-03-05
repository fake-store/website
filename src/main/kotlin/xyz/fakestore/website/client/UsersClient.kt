package xyz.fakestore.website.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val userId: String, val username: String, val email: String)
data class UserResponse(val userId: String, val username: String, val email: String)

@Component
class UsersClient(@Value("\${services.users.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun login(email: String, password: String): LoginResponse? =
        webClient.post()
            .uri("/api/users/login")
            .bodyValue(LoginRequest(email, password))
            .retrieve()
            .bodyToMono<LoginResponse>()
            .block()

    fun getMe(token: String): UserResponse? =
        webClient.get()
            .uri("/api/users/me")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono<UserResponse>()
            .block()
}

package xyz.fakestore.website.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class UserCountResponse(val count: Long)
data class AdminUserResponse(val userId: String, val username: String, val email: String)

@Component
class AdminClient(@Value("\${services.users.url}") baseUrl: String) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getUsers(): List<AdminUserResponse>? = runCatching {
        webClient.get()
            .uri("/api/admin/users")
            .retrieve()
            .bodyToMono<List<AdminUserResponse>>()
            .block()
    }.onFailure { log.warn("AdminClient.getUsers failed", it) }.getOrNull()

    fun getUserCount(): Long? = runCatching {
        webClient.get()
            .uri("/api/admin/users/count")
            .retrieve()
            .bodyToMono<UserCountResponse>()
            .block()?.count
    }.onFailure { log.warn("AdminClient.getUserCount failed", it) }.getOrNull()

    fun deleteAllUsers(): Long? = runCatching {
        webClient.delete()
            .uri("/api/admin/users")
            .retrieve()
            .bodyToMono<UserCountResponse>()
            .block()?.count
    }.onFailure { log.warn("AdminClient.deleteAllUsers failed", it) }.getOrNull()
}

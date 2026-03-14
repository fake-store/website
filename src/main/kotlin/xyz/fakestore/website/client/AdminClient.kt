package xyz.fakestore.website.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class UserCountResponse(val count: Long)

@Component
class AdminClient(@Value("\${services.users.url}") baseUrl: String) {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun getUserCount(): Long? = runCatching {
        webClient.get()
            .uri("/api/admin/users/count")
            .retrieve()
            .bodyToMono<UserCountResponse>()
            .block()?.count
    }.getOrNull()

    fun deleteAllUsers(): Long? = runCatching {
        webClient.delete()
            .uri("/api/admin/users")
            .retrieve()
            .bodyToMono<UserCountResponse>()
            .block()?.count
    }.getOrNull()
}

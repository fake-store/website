package xyz.fakestore.website.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigDecimal
import java.util.UUID

data class Product(
    val id: UUID,
    val title: String,
    val description: String?,
    val price: BigDecimal,
    val imagePath: String?
)

data class CreateProductRequest(
    val title: String,
    val description: String?,
    val price: BigDecimal
)

@Component
class CatalogClient(@Value("\${services.catalog.url}") baseUrl: String) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun search(query: String): List<Product>? = runCatching {
        webClient.get()
            .uri { it.path("/api/catalog/products").queryParam("q", query).build() }
            .retrieve()
            .bodyToMono<List<Product>>()
            .block()
    }.onFailure { log.warn("CatalogClient.search failed", it) }.getOrNull()

    fun getRecommended(userId: UUID?): List<Product>? = runCatching {
        webClient.get()
            .uri { b ->
                b.path("/api/catalog/recommended").apply {
                    if (userId != null) queryParam("userId", userId)
                }.build()
            }
            .retrieve()
            .bodyToMono<List<Product>>()
            .block()
    }.onFailure { log.warn("CatalogClient.getRecommended failed", it) }.getOrNull()

    fun createProduct(title: String, description: String?, price: BigDecimal): Product? = runCatching {
        webClient.post()
            .uri("/api/catalog/products")
            .bodyValue(CreateProductRequest(title, description, price))
            .retrieve()
            .bodyToMono<Product>()
            .block()
    }.onFailure { log.warn("CatalogClient.createProduct failed", it) }.getOrNull()
}

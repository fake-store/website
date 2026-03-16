package xyz.fakestore.website.web

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import xyz.fakestore.website.client.CartItemDto
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class CookieCartService {

    private val mapper = jacksonObjectMapper()
    private val cookieName = "fakestore_cart"

    fun getItems(request: HttpServletRequest): List<CartItemDto> {
        val cookie = request.cookies?.find { it.name == cookieName } ?: return emptyList()
        return runCatching {
            val json = URLDecoder.decode(cookie.value, StandardCharsets.UTF_8)
            mapper.readValue<List<CartItemDto>>(json)
        }.getOrDefault(emptyList())
    }

    fun addItem(item: CartItemDto, request: HttpServletRequest, response: HttpServletResponse) {
        val items = getItems(request).toMutableList()
        val idx = items.indexOfFirst { it.productId == item.productId }
        if (idx >= 0) {
            items[idx] = items[idx].copy(quantity = items[idx].quantity + item.quantity)
        } else {
            items.add(item)
        }
        writeCookie(items, response)
    }

    fun removeItem(productId: UUID, request: HttpServletRequest, response: HttpServletResponse) {
        val items = getItems(request).filter { it.productId != productId }
        writeCookie(items, response)
    }

    fun clear(response: HttpServletResponse) {
        val cookie = Cookie(cookieName, "")
        cookie.maxAge = 0
        cookie.path = "/"
        response.addCookie(cookie)
    }

    private fun writeCookie(items: List<CartItemDto>, response: HttpServletResponse) {
        val json = mapper.writeValueAsString(items)
        val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8)
        val cookie = Cookie(cookieName, encoded)
        cookie.maxAge = 30 * 24 * 60 * 60
        cookie.isHttpOnly = true
        cookie.path = "/"
        response.addCookie(cookie)
    }
}

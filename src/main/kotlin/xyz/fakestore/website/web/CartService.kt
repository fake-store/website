package xyz.fakestore.website.web

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val objectMapper: ObjectMapper
) {
    private val COOKIE_NAME = "fakestore_cart"
    private val COOKIE_MAX_AGE = 30 * 24 * 60 * 60

    fun getItems(userId: UUID?, request: HttpServletRequest): List<CartItem> =
        if (userId != null) cartRepository.getItems(userId)
        else getCookieItems(request)

    fun addItem(userId: UUID?, item: CartItem, request: HttpServletRequest, response: HttpServletResponse) {
        if (userId != null) {
            cartRepository.upsertItem(userId, item)
        } else {
            val items = getCookieItems(request).toMutableList()
            val idx = items.indexOfFirst { it.productId == item.productId }
            if (idx >= 0) {
                items[idx] = items[idx].copy(quantity = items[idx].quantity + 1)
            } else {
                items.add(item)
            }
            writeCookie(items, response)
        }
    }

    fun removeItem(userId: UUID?, productId: UUID, request: HttpServletRequest, response: HttpServletResponse) {
        if (userId != null) {
            cartRepository.removeItem(userId, productId)
        } else {
            writeCookie(getCookieItems(request).filter { it.productId != productId }, response)
        }
    }

    fun clearCart(userId: UUID?, request: HttpServletRequest, response: HttpServletResponse) {
        if (userId != null) {
            cartRepository.clearCart(userId)
        } else {
            deleteCookie(response)
        }
    }

    fun mergeAndClear(userId: UUID, request: HttpServletRequest, response: HttpServletResponse) {
        val cookieItems = getCookieItems(request)
        if (cookieItems.isEmpty()) return
        cookieItems.forEach { cartRepository.upsertItem(userId, it) }
        deleteCookie(response)
    }

    private fun getCookieItems(request: HttpServletRequest): List<CartItem> {
        val value = request.cookies?.find { it.name == COOKIE_NAME }?.value ?: return emptyList()
        return try {
            val json = URLDecoder.decode(value, StandardCharsets.UTF_8)
            objectMapper.readValue(json, object : TypeReference<List<CartItem>>() {})
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeCookie(items: List<CartItem>, response: HttpServletResponse) {
        val json = objectMapper.writeValueAsString(items)
        val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8)
        val cookie = Cookie(COOKIE_NAME, encoded)
        cookie.maxAge = COOKIE_MAX_AGE
        cookie.isHttpOnly = true
        cookie.path = "/"
        response.addCookie(cookie)
    }

    private fun deleteCookie(response: HttpServletResponse) {
        val cookie = Cookie(COOKIE_NAME, "")
        cookie.maxAge = 0
        cookie.path = "/"
        response.addCookie(cookie)
    }
}

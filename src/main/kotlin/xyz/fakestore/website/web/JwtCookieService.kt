package xyz.fakestore.website.web

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class TokenClaims(
    val token: String,
    val userId: String,
    val username: String,
    val email: String
)

@Component
class JwtCookieService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${cookie.secure:true}") private val secure: Boolean
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())
    private val cookieName = "fakestore_token"
    private val refreshCookieName = "fakestore_refresh"

    fun setToken(token: String, response: HttpServletResponse) {
        response.addCookie(Cookie(cookieName, token).apply {
            isHttpOnly = true
            secure = this@JwtCookieService.secure
            path = "/"
            maxAge = 60 * 60 * 24 * 30
            setAttribute("SameSite", "Lax")
        })
    }

    fun clearToken(response: HttpServletResponse) {
        response.addCookie(Cookie(cookieName, "").apply {
            isHttpOnly = true
            secure = this@JwtCookieService.secure
            path = "/"
            maxAge = 0
            setAttribute("SameSite", "Lax")
        })
    }

    fun setRefreshToken(refreshToken: String, response: HttpServletResponse) {
        response.addCookie(Cookie(refreshCookieName, refreshToken).apply {
            isHttpOnly = true
            secure = this@JwtCookieService.secure
            path = "/"
            maxAge = 60 * 60 * 24 * 30
            setAttribute("SameSite", "Lax")
        })
    }

    fun getRefreshToken(request: HttpServletRequest): String? =
        request.cookies?.find { it.name == refreshCookieName }?.value

    fun clearRefreshToken(response: HttpServletResponse) {
        response.addCookie(Cookie(refreshCookieName, "").apply {
            isHttpOnly = true
            secure = this@JwtCookieService.secure
            path = "/"
            maxAge = 0
            setAttribute("SameSite", "Lax")
        })
    }

    fun getToken(request: HttpServletRequest): String? =
        request.cookies?.find { it.name == cookieName }?.value

    fun getClaims(request: HttpServletRequest): TokenClaims? {
        val token = getToken(request) ?: return null
        return runCatching {
            val payload = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            TokenClaims(
                token = token,
                userId = payload.subject,
                username = payload["username"] as String,
                email = payload["email"] as String
            )
        }.onFailure { log.warn("Failed to parse JWT cookie", it) }.getOrNull()
    }
}

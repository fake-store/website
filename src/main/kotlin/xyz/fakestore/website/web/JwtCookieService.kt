package xyz.fakestore.website.web

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class TokenClaims(
    val token: String,
    val userId: String,
    val username: String,
    val email: String
)

@Component
class JwtCookieService(@Value("\${jwt.secret}") secret: String) {

    private val key = Keys.hmacShaKeyFor(secret.toByteArray())
    private val cookieName = "fakestore_token"

    fun setToken(token: String, response: HttpServletResponse) {
        response.addCookie(Cookie(cookieName, token).apply {
            isHttpOnly = true
            path = "/"
            maxAge = 60 * 60 * 24 * 30
        })
    }

    fun clearToken(response: HttpServletResponse) {
        response.addCookie(Cookie(cookieName, "").apply {
            isHttpOnly = true
            path = "/"
            maxAge = 0
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
        }.getOrNull()
    }
}

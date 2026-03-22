package xyz.fakestore.website.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import xyz.fakestore.website.client.UsersClient

/**
 * If the JWT cookie is stale (expired or invalid), attempts a silent refresh
 * using the refresh token cookie. On success, replaces both cookies with the
 * new pair. On failure, clears both cookies so the user is redirected to /login.
 */
@Component
@Order(1)
class StaleTokenFilter(
    private val jwtCookieService: JwtCookieService,
    private val usersClient: UsersClient
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (jwtCookieService.getToken(request) != null && jwtCookieService.getClaims(request) == null) {
            val refreshToken = jwtCookieService.getRefreshToken(request)
            if (refreshToken != null) {
                val refreshed = runCatching { usersClient.refresh(refreshToken) }
                    .onFailure { log.warn("Token refresh failed", it) }
                    .getOrNull()
                if (refreshed != null) {
                    jwtCookieService.setToken(refreshed.token, response)
                    jwtCookieService.setRefreshToken(refreshed.refreshToken, response)
                } else {
                    jwtCookieService.clearToken(response)
                    jwtCookieService.clearRefreshToken(response)
                }
            } else {
                jwtCookieService.clearToken(response)
            }
        }
        chain.doFilter(request, response)
    }
}

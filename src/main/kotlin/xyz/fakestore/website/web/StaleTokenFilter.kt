package xyz.fakestore.website.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Clears the fakestore_token cookie if it exists but fails validation
 * (expired, wrong secret, malformed). Prevents stale tokens from trapping
 * users in redirect loops where /login bounces them back to /.
 */
@Component
@Order(1)
class StaleTokenFilter(private val jwtCookieService: JwtCookieService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (jwtCookieService.getToken(request) != null && jwtCookieService.getClaims(request) == null) {
            jwtCookieService.clearToken(response)
        }
        chain.doFilter(request, response)
    }
}

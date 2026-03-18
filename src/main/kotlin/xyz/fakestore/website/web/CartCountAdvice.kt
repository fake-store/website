package xyz.fakestore.website.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import xyz.fakestore.website.client.CartClient

@ControllerAdvice
class CartCountAdvice(
    private val cartClient: CartClient,
    private val cookieCartService: CookieCartService,
    private val jwtCookieService: JwtCookieService
) {
    @ModelAttribute("cartCount")
    fun cartCount(request: HttpServletRequest): Int {
        val token = jwtCookieService.getToken(request)
        return if (token != null) {
            cartClient.getItems(token).size
        } else {
            cookieCartService.getItems(request).size
        }
    }
}

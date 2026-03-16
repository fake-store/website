package xyz.fakestore.website.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import xyz.fakestore.website.client.CartClient

@ControllerAdvice
class CartCountAdvice(
    private val cartClient: CartClient,
    private val cookieCartService: CookieCartService
) {
    @ModelAttribute("cartCount")
    fun cartCount(request: HttpServletRequest): Int {
        val token = request.getSession(false)?.getAttribute("token") as? String
        return if (token != null) {
            cartClient.getItems(token).size
        } else {
            cookieCartService.getItems(request).size
        }
    }
}

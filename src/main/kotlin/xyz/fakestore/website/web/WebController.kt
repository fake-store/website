package xyz.fakestore.website.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import xyz.fakestore.website.client.CatalogClient
import xyz.fakestore.website.client.CreateAddressRequest
import xyz.fakestore.website.client.OrdersClient
import xyz.fakestore.website.client.PaymentsClient
import xyz.fakestore.website.client.ShippingClient
import xyz.fakestore.website.client.UpdateAddressRequest
import xyz.fakestore.website.client.UsersClient
import java.math.BigDecimal
import java.util.UUID

@Controller
class WebController(
    private val usersClient: UsersClient,
    private val paymentsClient: PaymentsClient,
    private val ordersClient: OrdersClient,
    private val shippingClient: ShippingClient,
    private val catalogClient: CatalogClient,
    private val cartService: CartService
) {
    private val log = LoggerFactory.getLogger(WebController::class.java)

    @GetMapping("/")
    fun home(
        @RequestParam(required = false) q: String?,
        session: HttpSession,
        model: Model
    ): String {
        val token = session.getAttribute("token") as? String
        val username = session.getAttribute("username") as? String
        val userId = session.getAttribute("userId") as? String
        model.addAttribute("username", username)
        model.addAttribute("q", q)
        if (!q.isNullOrBlank()) {
            model.addAttribute("products", catalogClient.search(q))
        } else {
            model.addAttribute("products", catalogClient.getRecommended(userId?.let { UUID.fromString(it) }))
        }
        return "home"
    }

    @GetMapping("/me")
    fun mePage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        log.info("GET /me")
        val user = usersClient.getMe(token) ?: return "redirect:/logout"
        model.addAttribute("user", user)
        model.addAttribute("paymentMethods", paymentsClient.getMethods(token))
        model.addAttribute("recentPayments", paymentsClient.getHistory(token))
        model.addAttribute("paymentMethodTypes", listOf("CreditCard", "DebitCard", "Paypal", "ApplePay", "GooglePay", "Ach"))
        model.addAttribute("orders", ordersClient.getOrders(token)?.take(5))
        model.addAttribute("shippingAddresses", shippingClient.getAddresses(token))
        return "me"
    }

    @PostMapping("/me/update-email")
    fun updateEmail(
        @RequestParam email: String,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            usersClient.updateEmail(token, email) ?: throw RuntimeException("Update failed")
            redirectAttributes.addFlashAttribute("successEmail", "Email updated successfully")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorEmail", "Failed to update email. It may already be in use.")
            "redirect:/me"
        }
    }

    @PostMapping("/me/payments/add")
    fun addPaymentMethod(
        @RequestParam type: String,
        @RequestParam label: String,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            paymentsClient.addMethod(token, type, label) ?: throw RuntimeException("Add failed")
            redirectAttributes.addFlashAttribute("successPayments", "Payment method added")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorPayments", "Failed to add payment method")
            "redirect:/me"
        }
    }

    @PostMapping("/me/payments/{methodId}/update")
    fun updatePaymentMethod(
        @PathVariable methodId: UUID,
        @RequestParam label: String,
        @RequestParam(defaultValue = "false") isDefault: Boolean,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            paymentsClient.updateMethod(token, methodId, label, isDefault) ?: throw RuntimeException("Update failed")
            redirectAttributes.addFlashAttribute("successPayments", "Payment method updated")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorPayments", "Failed to update payment method")
            "redirect:/me"
        }
    }

    @PostMapping("/me/payments/{methodId}/delete")
    fun deletePaymentMethod(
        @PathVariable methodId: UUID,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            paymentsClient.deleteMethod(token, methodId)
            redirectAttributes.addFlashAttribute("successPayments", "Payment method removed")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorPayments", "Failed to remove payment method")
            "redirect:/me"
        }
    }

    @PostMapping("/me/shipping/add")
    fun addShippingAddress(
        @RequestParam label: String,
        @RequestParam street: String,
        @RequestParam city: String,
        @RequestParam state: String,
        @RequestParam postalCode: String,
        @RequestParam country: String,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            shippingClient.addAddress(token, CreateAddressRequest(label, street, city, state, postalCode, country))
                ?: throw RuntimeException("Add failed")
            redirectAttributes.addFlashAttribute("successShipping", "Address added")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorShipping", "Failed to add address")
            "redirect:/me"
        }
    }

    @PostMapping("/me/shipping/{id}/update")
    fun updateShippingAddress(
        @PathVariable id: UUID,
        @RequestParam label: String,
        @RequestParam street: String,
        @RequestParam city: String,
        @RequestParam state: String,
        @RequestParam postalCode: String,
        @RequestParam country: String,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            shippingClient.updateAddress(token, id, UpdateAddressRequest(label, street, city, state, postalCode, country))
                ?: throw RuntimeException("Update failed")
            redirectAttributes.addFlashAttribute("successShipping", "Address updated")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorShipping", "Failed to update address")
            "redirect:/me"
        }
    }

    @PostMapping("/me/shipping/{id}/delete")
    fun deleteShippingAddress(
        @PathVariable id: UUID,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        return try {
            shippingClient.deleteAddress(token, id)
            redirectAttributes.addFlashAttribute("successShipping", "Address removed")
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorShipping", "Failed to remove address")
            "redirect:/me"
        }
    }

    @GetMapping("/me/orders")
    fun ordersPage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("orders", ordersClient.getOrders(token))
        return "me-orders"
    }

    @PostMapping("/cart/add")
    fun addToCart(
        @RequestParam productId: UUID,
        @RequestParam title: String,
        @RequestParam price: BigDecimal,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        val userId = (session.getAttribute("userId") as? String)?.let { UUID.fromString(it) }
        cartService.addItem(userId, CartItem(productId, title, price), request, response)
        return "redirect:/"
    }

    @PostMapping("/cart/remove/{productId}")
    fun removeFromCart(
        @PathVariable productId: UUID,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        val userId = (session.getAttribute("userId") as? String)?.let { UUID.fromString(it) }
        cartService.removeItem(userId, productId, request, response)
        return "redirect:/cart"
    }

    @GetMapping("/cart")
    fun cartPage(
        session: HttpSession,
        model: Model,
        request: HttpServletRequest
    ): String {
        val userId = (session.getAttribute("userId") as? String)?.let { UUID.fromString(it) }
        val cart = cartService.getItems(userId, request)
        val total = cart.sumOf { it.price.multiply(BigDecimal(it.quantity)) }
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("cart", cart)
        model.addAttribute("total", total)
        return "cart"
    }

    @GetMapping("/checkout")
    fun checkoutPage(
        session: HttpSession,
        model: Model,
        request: HttpServletRequest
    ): String {
        val token = session.getAttribute("token") as? String
        val userId = (session.getAttribute("userId") as? String)?.let { UUID.fromString(it) }
        val cart = cartService.getItems(userId, request)
        if (cart.isEmpty()) return "redirect:/cart"
        val total = cart.sumOf { it.price.multiply(BigDecimal(it.quantity)) }
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("cart", cart)
        model.addAttribute("total", total)
        model.addAttribute("loggedIn", userId != null)
        if (token != null) {
            model.addAttribute("paymentMethods", paymentsClient.getMethods(token))
            model.addAttribute("shippingAddresses", shippingClient.getAddresses(token))
        }
        return "checkout"
    }

    @PostMapping("/checkout/place-order")
    fun placeOrder(
        @RequestParam paymentMethodId: UUID,
        @RequestParam shippingAddressId: UUID,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        val userId = (session.getAttribute("userId") as? String)?.let { UUID.fromString(it) }
            ?: return "redirect:/login"
        val cart = cartService.getItems(userId, request)
        if (cart.isEmpty()) return "redirect:/cart"
        return try {
            val orderResponse = ordersClient.placeOrder(token, userId, paymentMethodId, shippingAddressId, cart)
                ?: throw RuntimeException("Place order failed")
            cartService.clearCart(userId, request, response)
            "redirect:/me/orders/${orderResponse.orderId}"
        } catch (e: Exception) {
            log.error("Failed to place order", e)
            redirectAttributes.addFlashAttribute("errorOrder", "Failed to place order. Please try again.")
            "redirect:/checkout"
        }
    }

    @GetMapping("/me/orders/{orderId}")
    fun orderDetailPage(
        @PathVariable orderId: UUID,
        session: HttpSession,
        model: Model
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("orderId", orderId)
        model.addAttribute("order", ordersClient.getOrder(token, orderId))
        return "me-order-detail"
    }

    @GetMapping("/me/orders/{orderId}/status", produces = ["application/json"])
    @ResponseBody
    fun orderStatusJson(
        @PathVariable orderId: UUID,
        session: HttpSession
    ): ResponseEntity<*> {
        val token = session.getAttribute("token") as? String
            ?: return ResponseEntity.status(401).build<Any>()
        val status = ordersClient.getOrderStatus(token, orderId)
            ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(status)
    }

    @GetMapping("/login")
    fun loginPage(session: HttpSession): String {
        if (session.getAttribute("token") != null) return "redirect:/"
        return "login"
    }

    @PostMapping("/login")
    fun login(
        @RequestParam email: String,
        @RequestParam password: String,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            val loginResponse = usersClient.login(email, password)
                ?: throw RuntimeException("Login failed")
            session.setAttribute("token", loginResponse.token)
            session.setAttribute("username", loginResponse.username)
            session.setAttribute("userId", loginResponse.userId.toString())
            cartService.mergeAndClear(loginResponse.userId, request, response)
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password")
            "redirect:/login"
        }
    }

    @GetMapping("/register")
    fun registerPage(session: HttpSession): String {
        if (session.getAttribute("token") != null) return "redirect:/"
        return "register"
    }

    @PostMapping("/register")
    fun register(
        @RequestParam username: String,
        @RequestParam email: String,
        @RequestParam password: String,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            val registerResponse = usersClient.register(username, email, password)
                ?: throw RuntimeException("Registration failed")
            session.setAttribute("token", registerResponse.token)
            session.setAttribute("username", registerResponse.username)
            session.setAttribute("userId", registerResponse.userId.toString())
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Registration failed. The email may already be in use.")
            "redirect:/register"
        }
    }

    @GetMapping("/logout")
    fun logout(session: HttpSession): String {
        session.invalidate()
        return "redirect:/login"
    }

    @GetMapping("/admin/products/add")
    fun addProductPage(model: Model): String {
        return "admin/product-add"
    }

    @PostMapping("/admin/products/add")
    fun addProduct(
        @RequestParam title: String,
        @RequestParam(required = false) description: String?,
        @RequestParam price: BigDecimal,
        redirectAttributes: RedirectAttributes
    ): String {
        val product = catalogClient.createProduct(title, description, price)
        return if (product != null) {
            redirectAttributes.addFlashAttribute("success", "Product \"${product.title}\" added.")
            "redirect:/admin/products/add"
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to add product. Catalog service may be unavailable.")
            "redirect:/admin/products/add"
        }
    }
}

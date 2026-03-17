package xyz.fakestore.website.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import xyz.fakestore.website.client.CartClient
import xyz.fakestore.website.client.CartItemDto
import xyz.fakestore.website.client.CatalogClient
import xyz.fakestore.website.client.CreateAddressRequest
import xyz.fakestore.website.client.OrdersClient
import xyz.fakestore.website.client.PaymentsClient
import xyz.fakestore.website.client.PlaceOrderItem
import xyz.fakestore.website.client.PlaceOrderRequest
import xyz.fakestore.website.client.ShippingClient
import xyz.fakestore.website.client.UpdateAddressRequest
import xyz.fakestore.website.client.UsersClient
import xyz.fakestore.website.client.OrderDetail
import java.math.BigDecimal
import java.util.UUID

@Controller
class WebController(
    private val usersClient: UsersClient,
    private val paymentsClient: PaymentsClient,
    private val ordersClient: OrdersClient,
    private val shippingClient: ShippingClient,
    private val catalogClient: CatalogClient,
    private val cartClient: CartClient,
    private val cookieCartService: CookieCartService
) {
    private val log = LoggerFactory.getLogger(WebController::class.java)

    @GetMapping("/")
    fun home(
        @RequestParam(required = false) q: String?,
        session: HttpSession,
        model: Model
    ): String {
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
        model.addAttribute("recentOrders", ordersClient.getMe(token).take(5))
        model.addAttribute("recentPayments", paymentsClient.getHistory(token)?.take(5))
        return "me"
    }

    @GetMapping("/payments")
    fun paymentsPage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("payments", paymentsClient.getHistory(token))
        return "payments"
    }

    @GetMapping("/me/payment-methods")
    fun paymentMethodsPage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("paymentMethods", paymentsClient.getMethods(token))
        model.addAttribute("paymentMethodTypes", listOf("CreditCard", "DebitCard", "Paypal", "ApplePay", "GooglePay", "Ach"))
        return "me/payment-methods"
    }

    @GetMapping("/me/shipping")
    fun shippingPage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("shippingAddresses", shippingClient.getAddresses(token))
        return "me/shipping"
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
            "redirect:/me/payment-methods"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorPayments", "Failed to add payment method")
            "redirect:/me/payment-methods"
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
            "redirect:/me/payment-methods"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorPayments", "Failed to update payment method")
            "redirect:/me/payment-methods"
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
            "redirect:/me/payment-methods"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorPayments", "Failed to remove payment method")
            "redirect:/me/payment-methods"
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
            "redirect:/me/shipping"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorShipping", "Failed to add address")
            "redirect:/me/shipping"
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
            "redirect:/me/shipping"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorShipping", "Failed to update address")
            "redirect:/me/shipping"
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
            "redirect:/me/shipping"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorShipping", "Failed to remove address")
            "redirect:/me/shipping"
        }
    }

    @GetMapping("/me/orders")
    fun ordersPage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("orders", ordersClient.getMe(token))
        return "orders/index"
    }

    @GetMapping("/me/orders/{id}")
    fun orderDetailPage(
        @PathVariable id: UUID,
        session: HttpSession,
        model: Model
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        val order: OrderDetail = ordersClient.getOrderDetail(token, id) ?: return "redirect:/me/orders"
        val paymentMethod = paymentsClient.getMethods(token)?.find { it.userPaymentMethodId == order.paymentMethodId }
        val shippingAddress = shippingClient.getAddresses(token)?.find { it.id == order.shippingAddressId }
        model.addAttribute("username", session.getAttribute("username"))
        model.addAttribute("order", order)
        model.addAttribute("paymentMethod", paymentMethod)
        model.addAttribute("shippingAddress", shippingAddress)
        return "orders/detail"
    }

    // --- Cart ---

    @GetMapping("/cart")
    fun cartPage(
        session: HttpSession,
        request: HttpServletRequest,
        model: Model
    ): String {
        val token = session.getAttribute("token") as? String
        val username = session.getAttribute("username") as? String
        model.addAttribute("username", username)

        if (token != null) {
            val items = cartClient.getItems(token)
            model.addAttribute("items", items)
            model.addAttribute("total", items.sumOf { it.price.multiply(BigDecimal(it.quantity)) })
            model.addAttribute("paymentMethods", paymentsClient.getMethods(token))
            model.addAttribute("shippingAddresses", shippingClient.getAddresses(token))
            model.addAttribute("loggedIn", true)
        } else {
            val items = cookieCartService.getItems(request)
            model.addAttribute("items", items)
            model.addAttribute("total", items.sumOf { it.price.multiply(BigDecimal(it.quantity)) })
            model.addAttribute("loggedIn", false)
        }
        return "cart/index"
    }

    @PostMapping("/cart/add")
    fun addToCart(
        @RequestParam productId: UUID,
        @RequestParam title: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "1") quantity: Int,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String
        val item = CartItemDto(productId, title, price, quantity)
        if (token != null) {
            cartClient.addItem(token, item)
        } else {
            cookieCartService.addItem(item, request, response)
        }
        return "redirect:/cart"
    }

    @PostMapping("/cart/remove")
    fun removeFromCart(
        @RequestParam productId: UUID,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        val token = session.getAttribute("token") as? String
        if (token != null) {
            cartClient.removeItem(token, productId)
        } else {
            cookieCartService.removeItem(productId, request, response)
        }
        return "redirect:/cart"
    }

    @PostMapping("/cart/place-order")
    fun placeOrder(
        @RequestParam paymentMethodId: UUID,
        @RequestParam shippingAddressId: UUID,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        val userId = session.getAttribute("userId") as? String ?: return "redirect:/login"

        val items = cartClient.getItems(token)
        if (items.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty.")
            return "redirect:/cart"
        }

        val orderItems = items.map { PlaceOrderItem(it.productId, it.title, it.price, it.quantity) }
        val request = PlaceOrderRequest(UUID.fromString(userId), paymentMethodId, shippingAddressId, orderItems)

        return try {
            val placed = ordersClient.placeOrder(token, request) ?: throw RuntimeException("Order failed")
            cartClient.clearCart(token)
            "redirect:/me/orders/${placed.orderId}"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Failed to place order. Please try again.")
            "redirect:/cart"
        }
    }

    // --- Auth ---

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

            val cookieItems = cookieCartService.getItems(request)
            if (cookieItems.isNotEmpty()) {
                cartClient.mergeItems(loginResponse.token, cookieItems)
                cookieCartService.clear(response)
            }

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

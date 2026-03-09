package xyz.fakestore.website.web

import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import xyz.fakestore.website.client.CreateAddressRequest
import xyz.fakestore.website.client.OrdersClient
import xyz.fakestore.website.client.PaymentsClient
import xyz.fakestore.website.client.ShippingClient
import xyz.fakestore.website.client.UpdateAddressRequest
import xyz.fakestore.website.client.UsersClient
import java.util.UUID

@Controller
class WebController(
    private val usersClient: UsersClient,
    private val paymentsClient: PaymentsClient,
    private val ordersClient: OrdersClient,
    private val shippingClient: ShippingClient
) {

    @GetMapping("/")
    fun home(session: HttpSession): String {
        val token = session.getAttribute("token") as? String
        return if (token != null) "redirect:/me" else "redirect:/login"
    }

    @GetMapping("/me")
    fun mePage(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String ?: return "redirect:/login"
        val user = usersClient.getMe(token) ?: return "redirect:/logout"
        model.addAttribute("user", user)
        model.addAttribute("paymentMethods", paymentsClient.getMethods(token))
        model.addAttribute("paymentMethodTypes", listOf("CreditCard", "DebitCard", "Paypal", "ApplePay", "GooglePay", "Ach"))
        model.addAttribute("orders", ordersClient.getMe(token))
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

    @GetMapping("/login")
    fun loginPage(session: HttpSession): String {
        if (session.getAttribute("token") != null) return "redirect:/me"
        return "login"
    }

    @PostMapping("/login")
    fun login(
        @RequestParam email: String,
        @RequestParam password: String,
        session: HttpSession,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            val response = usersClient.login(email, password)
                ?: throw RuntimeException("Login failed")
            session.setAttribute("token", response.token)
            session.setAttribute("username", response.username)
            "redirect:/me"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password")
            "redirect:/login"
        }
    }

    @GetMapping("/register")
    fun registerPage(session: HttpSession): String {
        if (session.getAttribute("token") != null) return "redirect:/me"
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
            val response = usersClient.register(username, email, password)
                ?: throw RuntimeException("Registration failed")
            session.setAttribute("token", response.token)
            session.setAttribute("username", response.username)
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
}

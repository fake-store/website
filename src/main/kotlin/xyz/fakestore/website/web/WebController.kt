package xyz.fakestore.website.web

import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import xyz.fakestore.website.client.UsersClient

@Controller
class WebController(private val usersClient: UsersClient) {

    @GetMapping("/")
    fun home(session: HttpSession, model: Model): String {
        val token = session.getAttribute("token") as? String
        if (token != null) {
            val user = usersClient.getMe(token)
            if (user != null) {
                model.addAttribute("user", user)
                return "dashboard"
            }
        }
        return "redirect:/login"
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
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            val response = usersClient.login(email, password)
                ?: throw RuntimeException("Login failed")
            session.setAttribute("token", response.token)
            session.setAttribute("username", response.username)
            "redirect:/"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password")
            "redirect:/login"
        }
    }

    @GetMapping("/logout")
    fun logout(session: HttpSession): String {
        session.invalidate()
        return "redirect:/login"
    }
}

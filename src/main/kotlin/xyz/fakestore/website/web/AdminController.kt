package xyz.fakestore.website.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import xyz.fakestore.website.client.AdminClient

@Controller
@RequestMapping("/admin")
class AdminController(private val adminClient: AdminClient) {

    @GetMapping
    fun adminPage(model: Model): String {
        model.addAttribute("userCount", adminClient.getUserCount())
        return "admin"
    }

    @PostMapping("/users/delete-all")
    fun deleteAllUsers(redirectAttributes: RedirectAttributes): String {
        val deleted = adminClient.deleteAllUsers()
        if (deleted != null) {
            redirectAttributes.addFlashAttribute("message", "Deleted $deleted user(s).")
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to reach users service.")
        }
        return "redirect:/admin"
    }
}

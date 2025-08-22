package com.mindfultalk.growthlab.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.mindfultalk.growthlab.service.SuperAdminService;
import com.mindfultalk.growthlab.model.SuperAdmin;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/api/v1/swagger")
public class SwaggerAuthController {

    @Autowired
    private SuperAdminService superAdminService;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               Model model) {
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        return "swagger-login";
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestParam String userId,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        try {
            SuperAdmin superAdmin = superAdminService.findByUserId(userId);

            if (superAdmin != null && superAdminService.verifyPassword(password, superAdmin.getPassword())) {
                // Set session attributes for authentication
                session.setAttribute("superAdminAuthenticated", true);
                session.setAttribute("superAdminId", superAdmin.getId());
                session.setAttribute("superAdminUserId", superAdmin.getUserId());
                session.setMaxInactiveInterval(3600); // 1 hour session timeout

                // Redirect to Swagger UI
                return "redirect:/swagger-ui/index.html";
            } else {
                redirectAttributes.addAttribute("error", "true");
                return "redirect:/api/v1/swagger/login";
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "true");
            return "redirect:/api/v1/swagger/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/api/v1/swagger/login?logout=true";
    }

    @GetMapping("/unauthorized")
    public String unauthorized() {
        return "swagger-unauthorized";
    }

    // Health check endpoint to verify if user is still authenticated
    @GetMapping("/status")
    @ResponseBody
    public String checkAuthStatus(HttpSession session) {
        if (session != null && 
            session.getAttribute("superAdminAuthenticated") != null &&
            (Boolean) session.getAttribute("superAdminAuthenticated")) {
            return "authenticated";
        }
        return "unauthenticated";
    }
}
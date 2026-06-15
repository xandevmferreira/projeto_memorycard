package com.memorycard.controller.web;

import com.memorycard.dto.request.LoginRequest;
import com.memorycard.dto.request.RegisterRequest;
import com.memorycard.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthWebController {

    private final AuthService authService;

    public AuthWebController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest("", ""));
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                        BindingResult bindingResult,
                        HttpServletResponse response,
                        Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        try {
            var auth = authService.login(loginRequest);
            authService.setJwtCookie(response, auth.token());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest("", "", ""));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           HttpServletResponse response,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            var auth = authService.register(registerRequest);
            authService.setJwtCookie(response, auth.token());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        authService.clearJwtCookie(response);
        return "redirect:/login";
    }
}

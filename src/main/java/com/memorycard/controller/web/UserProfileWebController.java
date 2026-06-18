package com.memorycard.controller.web;

import com.memorycard.exception.AccessDeniedException;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.UserProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserProfileWebController {

    private final UserProfileService userProfileService;

    public UserProfileWebController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable("id") Long id, Model model) {
        try {
            var profile = userProfileService.getProfile(SecurityUtils.getCurrentUserId(), id);
            model.addAttribute("profile", profile);
            return "users/profile";
        } catch (AccessDeniedException e) {
            model.addAttribute("error", e.getMessage());
            return "users/denied";
        }
    }
}

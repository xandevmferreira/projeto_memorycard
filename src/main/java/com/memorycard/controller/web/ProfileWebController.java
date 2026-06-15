package com.memorycard.controller.web;

import com.memorycard.security.SecurityUtils;
import com.memorycard.service.CommunityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileWebController {

    private final CommunityService communityService;

    public ProfileWebController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        var user = SecurityUtils.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("hasRaApiKey", user.getRetroAchievementsApiKey() != null && !user.getRetroAchievementsApiKey().isBlank());
        return "profile/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(value = "communityVisible", defaultValue = "false") boolean communityVisible,
                                @RequestParam(value = "retroAchievementsUsername", required = false) String raUsername,
                                @RequestParam(value = "retroAchievementsApiKey", required = false) String raApiKey,
                                RedirectAttributes redirectAttributes) {
        communityService.updateProfile(
                SecurityUtils.getCurrentUserId(),
                communityVisible,
                raUsername,
                raApiKey
        );
        redirectAttributes.addFlashAttribute("success", "Perfil atualizado!");
        return "redirect:/profile";
    }
}

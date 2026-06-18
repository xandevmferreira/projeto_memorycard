package com.memorycard.controller.web;

import com.memorycard.security.SecurityUtils;
import com.memorycard.service.FriendService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/friends")
public class FriendWebController {

    private final FriendService friendService;

    public FriendWebController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public String index(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        model.addAttribute("friends", friendService.listFriends(userId));
        model.addAttribute("pendingIncoming", friendService.pendingIncoming(userId));
        model.addAttribute("pendingOutgoing", friendService.pendingOutgoing(userId));
        return "friends/index";
    }

    @PostMapping("/invite")
    public String invite(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            friendService.inviteByEmail(SecurityUtils.getCurrentUserId(), email);
            redirectAttributes.addFlashAttribute("success", "Convite enviado!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        friendService.accept(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Amizade confirmada!");
        return "redirect:/friends";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        friendService.reject(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Convite recusado.");
        return "redirect:/friends";
    }

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        friendService.removeFriend(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Amizade removida.");
        return "redirect:/friends";
    }
}

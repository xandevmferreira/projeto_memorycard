package com.memorycard.controller.web;

import com.memorycard.security.SecurityUtils;
import com.memorycard.service.PlaySessionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/games")
public class PlaySessionWebController {

    private final PlaySessionService playSessionService;

    public PlaySessionWebController(PlaySessionService playSessionService) {
        this.playSessionService = playSessionService;
    }

    @PostMapping("/{id}/play/start")
    public String start(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        playSessionService.startSession(SecurityUtils.getCurrentUserId(), id, "WEB", null);
        redirectAttributes.addFlashAttribute("success", "Contagem de horas iniciada!");
        return "redirect:/games/" + id;
    }

    @PostMapping("/{id}/play/stop")
    public String stop(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        playSessionService.stopSession(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Sessão encerrada — horas atualizadas!");
        return "redirect:/games/" + id;
    }
}

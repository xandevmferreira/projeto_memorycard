package com.memorycard.controller.web;

import com.memorycard.security.SecurityUtils;
import com.memorycard.service.GameListService;
import com.memorycard.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lists")
public class GameListWebController {

    private final GameListService gameListService;
    private final GameService gameService;

    public GameListWebController(GameListService gameListService, GameService gameService) {
        this.gameListService = gameListService;
        this.gameService = gameService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("lists", gameListService.findAllByUser(SecurityUtils.getCurrentUserId()));
        return "lists/index";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         @RequestParam(value = "description", required = false) String description,
                         RedirectAttributes redirectAttributes) {
        var list = gameListService.create(SecurityUtils.getCurrentUserId(), name, description);
        redirectAttributes.addFlashAttribute("success", "Lista criada!");
        return "redirect:/lists/" + list.id();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("list", gameListService.findById(SecurityUtils.getCurrentUserId(), id));
        model.addAttribute("games", gameService.findAllByUser(SecurityUtils.getCurrentUserId()));
        return "lists/detail";
    }

    @PostMapping("/{id}/add")
    public String addGame(@PathVariable("id") Long id,
                          @RequestParam("gameId") Long gameId,
                          RedirectAttributes redirectAttributes) {
        gameListService.addGame(SecurityUtils.getCurrentUserId(), id, gameId);
        redirectAttributes.addFlashAttribute("success", "Jogo adicionado à lista!");
        return "redirect:/lists/" + id;
    }

    @PostMapping("/{listId}/remove/{gameId}")
    public String removeGame(@PathVariable("listId") Long listId,
                             @PathVariable("gameId") Long gameId,
                             RedirectAttributes redirectAttributes) {
        gameListService.removeGame(SecurityUtils.getCurrentUserId(), listId, gameId);
        redirectAttributes.addFlashAttribute("success", "Jogo removido da lista!");
        return "redirect:/lists/" + listId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        gameListService.delete(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Lista removida!");
        return "redirect:/lists";
    }
}

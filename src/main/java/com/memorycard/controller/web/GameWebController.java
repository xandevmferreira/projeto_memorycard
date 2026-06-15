package com.memorycard.controller.web;

import com.memorycard.dto.request.GameRequest;
import com.memorycard.entity.GameStatus;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.CoverImageService;
import com.memorycard.service.ExternalGameApiService;
import com.memorycard.service.GameService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/games")
public class GameWebController {

    private final GameService gameService;
    private final ExternalGameApiService externalGameApiService;
    private final CoverImageService coverImageService;

    public GameWebController(GameService gameService,
                             ExternalGameApiService externalGameApiService,
                             CoverImageService coverImageService) {
        this.gameService = gameService;
        this.externalGameApiService = externalGameApiService;
        this.coverImageService = coverImageService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("games", gameService.findAllByUser(SecurityUtils.getCurrentUserId()));
        return "games/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("gameForm", new GameForm());
        model.addAttribute("statuses", GameStatus.values());
        model.addAttribute("isEdit", false);
        return "games/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("gameForm") GameForm gameForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "cover", required = false) MultipartFile cover,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", GameStatus.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("existingCover", coverImageService.toDisplayUrl(gameForm.getExternalCoverUrl()));
            return "games/form";
        }
        var game = gameService.create(SecurityUtils.getCurrentUserId(), gameForm.toRequest(), cover, gameForm.getExternalCoverUrl());
        redirectAttributes.addFlashAttribute("success", "Jogo cadastrado com sucesso!");
        return "redirect:/games/" + game.id();
    }

    @GetMapping("/search-external")
    @ResponseBody
    public Object searchExternal(@RequestParam("query") String query) {
        return externalGameApiService.searchGames(query, 12);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("game", gameService.findById(SecurityUtils.getCurrentUserId(), id));
        return "games/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        var game = gameService.findById(SecurityUtils.getCurrentUserId(), id);
        String rawCoverUrl = resolveRawCoverUrl(game.coverUrl());

        GameForm form = new GameForm();
        form.setTitle(game.title());
        form.setPlatform(game.platform());
        form.setStatus(game.status());
        form.setHoursPlayed(game.hoursPlayed() != null ? game.hoursPlayed() : BigDecimal.ZERO);
        form.setPersonalRating(game.personalRating());
        form.setExternalRating(game.externalRating());
        form.setNotes(game.notes());
        form.setStartedAt(game.startedAt());
        form.setCompletedAt(game.completedAt());
        form.setExternalCoverUrl(rawCoverUrl != null && rawCoverUrl.startsWith("/uploads") ? "" : (rawCoverUrl != null ? rawCoverUrl : ""));

        model.addAttribute("gameForm", form);
        model.addAttribute("gameId", id);
        model.addAttribute("existingCover", rawCoverUrl != null && rawCoverUrl.startsWith("/uploads")
                ? rawCoverUrl
                : coverImageService.toDisplayUrl(rawCoverUrl));
        model.addAttribute("statuses", GameStatus.values());
        model.addAttribute("isEdit", true);
        return "games/form";
    }

    @GetMapping("/{id}/complete")
    public String markAsCompleted(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        gameService.markAsCompleted(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Jogo marcado como zerado!");
        return "redirect:/games/" + id;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("gameForm") GameForm gameForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "cover", required = false) MultipartFile cover,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("gameId", id);
            model.addAttribute("statuses", GameStatus.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("existingCover", coverImageService.toDisplayUrl(gameForm.getExternalCoverUrl()));
            return "games/form";
        }
        gameService.update(SecurityUtils.getCurrentUserId(), id, gameForm.toRequest(), cover, gameForm.getExternalCoverUrl());
        redirectAttributes.addFlashAttribute("success", "Jogo atualizado com sucesso!");
        return "redirect:/games/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        gameService.delete(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Jogo removido com sucesso!");
        return "redirect:/games";
    }

    @PostMapping("/{id}/screenshots")
    public String uploadScreenshot(@PathVariable("id") Long id,
                                   @RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        gameService.addScreenshot(SecurityUtils.getCurrentUserId(), id, file);
        redirectAttributes.addFlashAttribute("success", "Screenshot enviado!");
        return "redirect:/games/" + id;
    }

    @PostMapping("/{gameId}/screenshots/{screenshotId}/delete")
    public String deleteScreenshot(@PathVariable("gameId") Long gameId,
                                   @PathVariable("screenshotId") Long screenshotId,
                                   RedirectAttributes redirectAttributes) {
        gameService.deleteScreenshot(SecurityUtils.getCurrentUserId(), gameId, screenshotId);
        redirectAttributes.addFlashAttribute("success", "Screenshot removido!");
        return "redirect:/games/" + gameId;
    }

    private String resolveRawCoverUrl(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (coverUrl.startsWith("/covers/proxy?url=")) {
            String encoded = coverUrl.substring("/covers/proxy?url=".length());
            return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
        }
        return coverUrl;
    }
}

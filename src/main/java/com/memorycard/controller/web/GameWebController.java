package com.memorycard.controller.web;

import com.memorycard.dto.request.GameRequest;
import com.memorycard.entity.CompletionType;
import com.memorycard.entity.GameStatus;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.CoverImageService;
import com.memorycard.service.ExternalGameApiService;
import com.memorycard.service.GameJournalService;
import com.memorycard.service.GameService;
import com.memorycard.service.RetroAchievementsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

import com.memorycard.util.MetacriticFormatter;

@Controller
@RequestMapping("/games")
public class GameWebController {

    private final GameService gameService;
    private final ExternalGameApiService externalGameApiService;
    private final CoverImageService coverImageService;
    private final RetroAchievementsService retroAchievementsService;
    private final GameJournalService gameJournalService;

    public GameWebController(GameService gameService,
                             ExternalGameApiService externalGameApiService,
                             CoverImageService coverImageService,
                             RetroAchievementsService retroAchievementsService,
                             GameJournalService gameJournalService) {
        this.gameService = gameService;
        this.externalGameApiService = externalGameApiService;
        this.coverImageService = coverImageService;
        this.retroAchievementsService = retroAchievementsService;
        this.gameJournalService = gameJournalService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("games", gameService.findAllByUser(SecurityUtils.getCurrentUserId()));
        return "games/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("gameForm", new GameForm());
        populateFormOptions(model);
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
            populateFormOptions(model);
            model.addAttribute("isEdit", false);
            model.addAttribute("existingCover", coverImageService.toDisplayUrl(gameForm.getExternalCoverUrl()));
            return "games/form";
        }
        var game = gameService.create(SecurityUtils.getCurrentUserId(), gameForm.toRequest(), cover, gameForm.getExternalCoverUrl());
        redirectAttributes.addFlashAttribute("success", "Jogo cadastrado com sucesso!");
        return "redirect:/games/" + game.id();
    }

    @GetMapping("/search-retro")
    @ResponseBody
    public Object searchRetro(@RequestParam("query") String query) {
        return retroAchievementsService.search(query, 10);
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
        form.setNotes(game.notes());
        form.setStartedAt(game.startedAt());
        form.setCompletedAt(game.completedAt());
        form.setCompletionType(game.completionType());
        form.setTags(game.tags() != null ? game.tags() : "");
        form.setRetro(game.retro());
        form.setEmulator(game.emulator() != null ? game.emulator() : "");
        form.setRetroAchievementsGameId(game.retroAchievementsGameId());
        form.setRetroConsoleId(game.retroConsoleId());
        form.setExternalCoverUrl(rawCoverUrl != null && rawCoverUrl.startsWith("/uploads") ? "" : (rawCoverUrl != null ? rawCoverUrl : ""));

        model.addAttribute("gameForm", form);
        model.addAttribute("gameId", id);
        model.addAttribute("existingCover", rawCoverUrl != null && rawCoverUrl.startsWith("/uploads")
                ? rawCoverUrl
                : coverImageService.toDisplayUrl(rawCoverUrl));
        model.addAttribute("metacriticDisplay", MetacriticFormatter.format(game.externalRating(), game.ratingSource()));
        populateFormOptions(model);
        model.addAttribute("isEdit", true);
        return "games/form";
    }

    @GetMapping("/{id}/sync-retro")
    public String syncRetro(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        gameService.syncRetroProgressForGame(SecurityUtils.getCurrentUserId(), id);
        redirectAttributes.addFlashAttribute("success", "Progresso RetroAchievements atualizado!");
        return "redirect:/games/" + id;
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
            populateFormOptions(model);
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

    @PostMapping("/{id}/journal")
    public String addJournal(@PathVariable("id") Long id,
                             @RequestParam("content") String content,
                             @RequestParam(value = "spoiler", defaultValue = "false") boolean spoiler,
                             RedirectAttributes redirectAttributes) {
        gameJournalService.addEntry(SecurityUtils.getCurrentUserId(), id, content, spoiler);
        redirectAttributes.addFlashAttribute("success", "Entrada adicionada ao diário!");
        return "redirect:/games/" + id;
    }

    @PostMapping("/{gameId}/journal/{entryId}/delete")
    public String deleteJournal(@PathVariable("gameId") Long gameId,
                                @PathVariable("entryId") Long entryId,
                                RedirectAttributes redirectAttributes) {
        gameJournalService.deleteEntry(SecurityUtils.getCurrentUserId(), gameId, entryId);
        redirectAttributes.addFlashAttribute("success", "Entrada removida!");
        return "redirect:/games/" + gameId;
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("statuses", GameStatus.values());
        model.addAttribute("completionTypes", CompletionType.values());
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

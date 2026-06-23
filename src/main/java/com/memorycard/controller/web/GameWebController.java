package com.memorycard.controller.web;

import com.memorycard.dto.request.GameRequest;
import com.memorycard.entity.CompletionType;
import com.memorycard.entity.GameStatus;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.CoverImageService;
import com.memorycard.service.ExternalGameApiService;
import com.memorycard.service.GameCartridgeService;
import com.memorycard.service.GameJournalService;
import com.memorycard.service.GameService;
import com.memorycard.service.PlaySessionService;
import com.memorycard.service.RetroAchievementsService;
import com.memorycard.service.SupportedPlatforms;
import com.memorycard.entity.ArchiveFileType;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
    private final PlaySessionService playSessionService;
    private final GameCartridgeService gameCartridgeService;

    public GameWebController(GameService gameService,
                             ExternalGameApiService externalGameApiService,
                             CoverImageService coverImageService,
                             RetroAchievementsService retroAchievementsService,
                             GameJournalService gameJournalService,
                             PlaySessionService playSessionService,
                             GameCartridgeService gameCartridgeService) {
        this.gameService = gameService;
        this.externalGameApiService = externalGameApiService;
        this.coverImageService = coverImageService;
        this.retroAchievementsService = retroAchievementsService;
        this.gameJournalService = gameJournalService;
        this.playSessionService = playSessionService;
        this.gameCartridgeService = gameCartridgeService;
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
        Long userId = SecurityUtils.getCurrentUserId();
        var game = gameService.findById(userId, id);
        var cartridges = gameCartridgeService.listForGame(userId, id);
        boolean webPlayer = SupportedPlatforms.supportsWebPlayer(game.platform());

        model.addAttribute("game", game);
        model.addAttribute("activePlaySession", playSessionService.findActiveForGame(userId, id));
        model.addAttribute("cartridges", cartridges);
        model.addAttribute("archiveFileTypes", ArchiveFileType.values());
        model.addAttribute("webPlayerSupported", webPlayer);
        model.addAttribute("latestCartridge", cartridges.isEmpty() ? null : cartridges.getFirst());
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

    @PostMapping("/{id}/cartridges/record")
    public String recordTape(@PathVariable("id") Long id,
                             @RequestParam("label") String label,
                             @RequestParam(value = "memories", required = false) String memories,
                             @RequestParam(value = "sessionDate", required = false) String sessionDate,
                             @RequestParam(value = "emulatorHint", required = false) String emulatorHint,
                             @RequestParam(value = "saveFile", required = false) MultipartFile saveFile,
                             @RequestParam(value = "stateFile", required = false) MultipartFile stateFile,
                             @RequestParam(value = "screenshot", required = false) MultipartFile screenshot,
                             @RequestParam(value = "extraFiles", required = false) MultipartFile[] extraFiles,
                             RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDate date = sessionDate != null && !sessionDate.isBlank()
                    ? java.time.LocalDate.parse(sessionDate)
                    : java.time.LocalDate.now();
            var cartridge = gameCartridgeService.recordTape(
                    SecurityUtils.getCurrentUserId(), id, label, memories, date, emulatorHint,
                    saveFile, stateFile, screenshot, extraFiles);
            redirectAttributes.addFlashAttribute("success", "Fita gravada com sucesso!");
            return "redirect:/games/" + id + "/cartridges/" + cartridge.id() + "/insert";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/games/" + id + "#fitas";
        }
    }

    @GetMapping("/{gameId}/cartridges/{cartridgeId}/insert")
    public String insertTape(@PathVariable("gameId") Long gameId,
                             @PathVariable("cartridgeId") Long cartridgeId,
                             Model model) {
        model.addAttribute("tape", gameCartridgeService.getInsertView(
                SecurityUtils.getCurrentUserId(), gameId, cartridgeId));
        return "games/cartridge-insert";
    }

    @GetMapping("/{gameId}/cartridges/{cartridgeId}/play")
    public String playTape(@PathVariable("gameId") Long gameId,
                           @PathVariable("cartridgeId") Long cartridgeId,
                           Model model) {
        try {
            model.addAttribute("manifest", gameCartridgeService.getPlayerManifest(
                    SecurityUtils.getCurrentUserId(), gameId, cartridgeId));
            return "games/cartridge-play";
        } catch (IllegalArgumentException e) {
            return "redirect:/games/" + gameId + "/cartridges/" + cartridgeId + "/insert";
        }
    }

    @GetMapping("/{gameId}/cartridges/{cartridgeId}/export")
    public ResponseEntity<Resource> exportTape(@PathVariable("gameId") Long gameId,
                                               @PathVariable("cartridgeId") Long cartridgeId) throws IOException {
        Resource zip = gameCartridgeService.exportCartridgeZip(
                SecurityUtils.getCurrentUserId(), gameId, cartridgeId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zip.getFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    @PostMapping("/{id}/cartridges")
    public String createCartridge(@PathVariable("id") Long id,
                                  @RequestParam("label") String label,
                                  @RequestParam(value = "memories", required = false) String memories,
                                  @RequestParam(value = "sessionDate", required = false) String sessionDate,
                                  @RequestParam(value = "emulatorHint", required = false) String emulatorHint,
                                  RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDate date = sessionDate != null && !sessionDate.isBlank()
                    ? java.time.LocalDate.parse(sessionDate)
                    : null;
            gameCartridgeService.create(SecurityUtils.getCurrentUserId(), id, label, memories, date, emulatorHint);
            redirectAttributes.addFlashAttribute("success", "Fita digital criada!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/games/" + id;
    }

    @PostMapping("/{gameId}/cartridges/{cartridgeId}/files")
    public String uploadArchiveFile(@PathVariable("gameId") Long gameId,
                                    @PathVariable("cartridgeId") Long cartridgeId,
                                    @RequestParam("fileType") ArchiveFileType fileType,
                                    @RequestParam("file") MultipartFile file,
                                    RedirectAttributes redirectAttributes) {
        try {
            gameCartridgeService.addFile(SecurityUtils.getCurrentUserId(), gameId, cartridgeId, fileType, file);
            redirectAttributes.addFlashAttribute("success", "Arquivo adicionado à fita!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/games/" + gameId;
    }

    @GetMapping("/{gameId}/cartridges/{cartridgeId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadArchiveFile(@PathVariable("gameId") Long gameId,
                                                        @PathVariable("cartridgeId") Long cartridgeId,
                                                        @PathVariable("fileId") Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Resource resource = gameCartridgeService.downloadFile(userId, gameId, cartridgeId, fileId);
        String filename = gameCartridgeService.getOriginalFilename(userId, gameId, cartridgeId, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/{gameId}/cartridges/{cartridgeId}/delete")
    public String deleteCartridge(@PathVariable("gameId") Long gameId,
                                  @PathVariable("cartridgeId") Long cartridgeId,
                                  RedirectAttributes redirectAttributes) {
        gameCartridgeService.deleteCartridge(SecurityUtils.getCurrentUserId(), gameId, cartridgeId);
        redirectAttributes.addFlashAttribute("success", "Fita removida.");
        return "redirect:/games/" + gameId;
    }

    @PostMapping("/{gameId}/cartridges/{cartridgeId}/files/{fileId}/delete")
    public String deleteArchiveFile(@PathVariable("gameId") Long gameId,
                                    @PathVariable("cartridgeId") Long cartridgeId,
                                    @PathVariable("fileId") Long fileId,
                                    RedirectAttributes redirectAttributes) {
        gameCartridgeService.deleteFile(SecurityUtils.getCurrentUserId(), gameId, cartridgeId, fileId);
        redirectAttributes.addFlashAttribute("success", "Arquivo removido.");
        return "redirect:/games/" + gameId;
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("statuses", GameStatus.values());
        model.addAttribute("completionTypes", CompletionType.values());
        model.addAttribute("platformSuggestions", SupportedPlatforms.forForm());
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

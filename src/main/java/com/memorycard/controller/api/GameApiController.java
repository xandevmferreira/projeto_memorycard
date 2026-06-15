package com.memorycard.controller.api;

import com.memorycard.dto.request.GameRequest;
import com.memorycard.dto.response.DashboardResponse;
import com.memorycard.dto.response.ExternalGameInfo;
import com.memorycard.dto.response.GameResponse;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.DashboardService;
import com.memorycard.service.ExternalGameApiService;
import com.memorycard.service.GamingNewsService;
import com.memorycard.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GameApiController {

    private final GameService gameService;
    private final DashboardService dashboardService;
    private final ExternalGameApiService externalGameApiService;
    private final GamingNewsService gamingNewsService;

    public GameApiController(GameService gameService, DashboardService dashboardService,
                             ExternalGameApiService externalGameApiService,
                             GamingNewsService gamingNewsService) {
        this.gameService = gameService;
        this.dashboardService = dashboardService;
        this.externalGameApiService = externalGameApiService;
        this.gamingNewsService = gamingNewsService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/games")
    public List<GameResponse> listGames() {
        return gameService.findAllByUser(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/games/{id}")
    public GameResponse getGame(@PathVariable("id") Long id) {
        return gameService.findById(SecurityUtils.getCurrentUserId(), id);
    }

    @PostMapping(value = "/games", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GameResponse createGame(@Valid @RequestPart("game") GameRequest request,
                                   @RequestPart(value = "cover", required = false) MultipartFile cover,
                                   @RequestParam(value = "externalCoverUrl", required = false) String externalCoverUrl) {
        return gameService.create(SecurityUtils.getCurrentUserId(), request, cover, externalCoverUrl);
    }

    @PutMapping(value = "/games/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GameResponse updateGame(@PathVariable("id") Long id,
                                   @Valid @RequestPart("game") GameRequest request,
                                   @RequestPart(value = "cover", required = false) MultipartFile cover,
                                   @RequestParam(value = "externalCoverUrl", required = false) String externalCoverUrl) {
        return gameService.update(SecurityUtils.getCurrentUserId(), id, request, cover, externalCoverUrl);
    }

    @DeleteMapping("/games/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable("id") Long id) {
        gameService.delete(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/games/{id}/screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GameService.ScreenshotUploadResult uploadScreenshot(@PathVariable("id") Long id,
                                                                @RequestPart("file") MultipartFile file) {
        return gameService.addScreenshot(SecurityUtils.getCurrentUserId(), id, file);
    }

    @DeleteMapping("/games/{gameId}/screenshots/{screenshotId}")
    public ResponseEntity<Void> deleteScreenshot(@PathVariable("gameId") Long gameId,
                                                  @PathVariable("screenshotId") Long screenshotId) {
        gameService.deleteScreenshot(SecurityUtils.getCurrentUserId(), gameId, screenshotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/external-games/search")
    public List<ExternalGameInfo> searchExternalGames(@RequestParam("query") String query,
                                                       @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return externalGameApiService.searchGames(query, limit);
    }

    @GetMapping("/news")
    public List<com.memorycard.dto.response.GamingNewsItem> gamingNews() {
        return gamingNewsService.getLatestNews();
    }
}

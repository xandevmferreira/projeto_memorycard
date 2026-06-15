package com.memorycard.service;

import com.memorycard.dto.request.GameRequest;
import com.memorycard.dto.response.GameResponse;
import com.memorycard.entity.Game;
import com.memorycard.entity.GameScreenshot;
import com.memorycard.entity.GameStatus;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameRepository;
import com.memorycard.repository.GameScreenshotRepository;
import com.memorycard.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameScreenshotRepository screenshotRepository;
    private final StorageService storageService;
    private final ExternalGameApiService externalGameApiService;
    private final CoverImageService coverImageService;

    public GameService(GameRepository gameRepository,
                       GameScreenshotRepository screenshotRepository,
                       StorageService storageService,
                       ExternalGameApiService externalGameApiService,
                       CoverImageService coverImageService) {
        this.gameRepository = gameRepository;
        this.screenshotRepository = screenshotRepository;
        this.storageService = storageService;
        this.externalGameApiService = externalGameApiService;
        this.coverImageService = coverImageService;
    }

    @Transactional(readOnly = true)
    public List<GameResponse> findAllByUser(Long userId) {
        return gameRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(game -> GameMapper.toResponse(game, storageService, coverImageService))
                .toList();
    }

    @Transactional(readOnly = true)
    public GameResponse findById(Long userId, Long gameId) {
        Game game = getOwnedGame(userId, gameId);
        List<GameScreenshot> screenshots = screenshotRepository.findByGameIdOrderByUploadedAtDesc(gameId);
        return GameMapper.toResponse(game, screenshots, storageService, coverImageService);
    }

    @Transactional
    public GameResponse create(Long userId, GameRequest request, MultipartFile cover, String externalCoverUrl) {
        Game game = new Game();
        game.setUserId(userId);
        applyRequest(game, request);
        enrichFromExternalApi(game, request.title());
        applyCover(game, userId, cover, externalCoverUrl);

        Game savedGame = gameRepository.save(game);
        return GameMapper.toResponse(savedGame, storageService, coverImageService);
    }

    @Transactional
    public GameResponse update(Long userId, Long gameId, GameRequest request, MultipartFile cover, String externalCoverUrl) {
        Game game = getOwnedGame(userId, gameId);
        applyRequest(game, request);

        boolean hasUploadedCover = cover != null && !cover.isEmpty();
        boolean hasSelectedCover = externalCoverUrl != null && !externalCoverUrl.isBlank();

        if (!hasUploadedCover && !hasSelectedCover) {
            refreshCoverFromTitle(game);
        }

        applyCover(game, userId, cover, externalCoverUrl);

        Game savedGame = gameRepository.save(game);
        List<GameScreenshot> screenshots = screenshotRepository.findByGameIdOrderByUploadedAtDesc(gameId);
        return GameMapper.toResponse(savedGame, screenshots, storageService, coverImageService);
    }

    private void applyCover(Game game, Long userId, MultipartFile cover, String externalCoverUrl) {
        if (cover != null && !cover.isEmpty()) {
            String path = storageService.store(cover, "covers/" + userId);
            game.setCoverUrl(storageService.getPublicUrl(path));
            return;
        }

        String sourceUrl = normalizeExternalCoverUrl(externalCoverUrl);
        if (sourceUrl != null && coverImageService.isExternalUrl(sourceUrl)) {
            persistAndSetCover(game, userId, sourceUrl);
            return;
        }

        if (game.getCoverUrl() != null && game.getCoverUrl().startsWith("/uploads")) {
            return;
        }

        if (game.getCoverUrl() != null && coverImageService.isExternalUrl(game.getCoverUrl())) {
            persistAndSetCover(game, userId, game.getCoverUrl());
        }
    }

    private void persistAndSetCover(Game game, Long userId, String sourceUrl) {
        String localPath = coverImageService.persistFromUrl(sourceUrl, userId);
        if (localPath != null) {
            game.setCoverUrl(storageService.getPublicUrl(localPath));
            return;
        }

        refreshCoverFromTitle(game);
        if (game.getCoverUrl() != null && coverImageService.isExternalUrl(game.getCoverUrl())) {
            localPath = coverImageService.persistFromUrl(game.getCoverUrl(), userId);
            if (localPath != null) {
                game.setCoverUrl(storageService.getPublicUrl(localPath));
            } else {
                game.setCoverUrl(null);
            }
        }
    }

    private void refreshCoverFromTitle(Game game) {
        externalGameApiService.searchByTitle(game.getTitle()).ifPresent(info -> {
            if (info.coverUrl() != null) {
                game.setCoverUrl(info.coverUrl());
            }
        });
    }

    private String normalizeExternalCoverUrl(String externalCoverUrl) {
        if (externalCoverUrl == null || externalCoverUrl.isBlank()) {
            return null;
        }
        String value = externalCoverUrl.trim();
        if (value.startsWith("/covers/proxy?url=")) {
            String encoded = value.substring("/covers/proxy?url=".length());
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        }
        return value;
    }

    private void enrichFromExternalApi(Game game, String title) {
        externalGameApiService.searchByTitle(title).ifPresent(info -> {
            if (game.getExternalRating() == null && info.externalRating() != null) {
                game.setExternalRating(info.externalRating());
            }
            if (game.getCoverUrl() == null && info.coverUrl() != null) {
                game.setCoverUrl(info.coverUrl());
            }
            if (game.getPlatform() == null && info.platform() != null) {
                game.setPlatform(info.platform());
            }
        });
    }

    @Transactional
    public GameResponse markAsCompleted(Long userId, Long gameId) {
        Game game = getOwnedGame(userId, gameId);
        game.setStatus(GameStatus.COMPLETED);
        if (game.getCompletedAt() == null) {
            game.setCompletedAt(java.time.LocalDate.now());
        }
        Game savedGame = gameRepository.save(game);
        List<GameScreenshot> screenshots = screenshotRepository.findByGameIdOrderByUploadedAtDesc(gameId);
        return GameMapper.toResponse(savedGame, screenshots, storageService, coverImageService);
    }

    @Transactional
    public void delete(Long userId, Long gameId) {
        Game game = getOwnedGame(userId, gameId);
        List<GameScreenshot> screenshots = screenshotRepository.findByGameIdOrderByUploadedAtDesc(gameId);
        for (GameScreenshot screenshot : screenshots) {
            storageService.delete(screenshot.getFilePath());
        }
        gameRepository.delete(game);
    }

    @Transactional
    public ScreenshotUploadResult addScreenshot(Long userId, Long gameId, MultipartFile file) {
        getOwnedGame(userId, gameId);
        String path = storageService.store(file, "screenshots/" + userId + "/" + gameId);

        GameScreenshot screenshot = new GameScreenshot();
        screenshot.setGameId(gameId);
        screenshot.setFilePath(path);
        screenshot = screenshotRepository.save(screenshot);

        return new ScreenshotUploadResult(
                screenshot.getId(),
                storageService.getPublicUrl(path),
                screenshot.getUploadedAt()
        );
    }

    @Transactional
    public void deleteScreenshot(Long userId, Long gameId, Long screenshotId) {
        getOwnedGame(userId, gameId);
        GameScreenshot screenshot = screenshotRepository.findByIdAndGameId(screenshotId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Screenshot não encontrado"));
        storageService.delete(screenshot.getFilePath());
        screenshotRepository.delete(screenshot);
    }

    private Game getOwnedGame(Long userId, Long gameId) {
        return gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));
    }

    private void applyRequest(Game game, GameRequest request) {
        game.setTitle(request.title());
        game.setPlatform(request.platform());
        game.setHoursPlayed(request.hoursPlayed() != null ? request.hoursPlayed() : BigDecimal.ZERO);
        game.setPersonalRating(request.personalRating());
        game.setExternalRating(request.externalRating());
        game.setNotes(request.notes());
        game.setStartedAt(request.startedAt());

        if (request.completedAt() != null) {
            game.setStatus(GameStatus.COMPLETED);
            game.setCompletedAt(request.completedAt());
        } else {
            game.setStatus(request.status());
            if (request.status() == GameStatus.COMPLETED) {
                game.setCompletedAt(game.getCompletedAt() != null
                        ? game.getCompletedAt()
                        : java.time.LocalDate.now());
            } else {
                game.setCompletedAt(null);
            }
        }
    }

    public record ScreenshotUploadResult(Long id, String url, java.time.Instant uploadedAt) {}
}

package com.memorycard.service;

import com.memorycard.dto.response.GameResponse;
import com.memorycard.dto.response.ScreenshotResponse;
import com.memorycard.entity.Game;
import com.memorycard.entity.GameScreenshot;
import com.memorycard.storage.StorageService;

import java.util.Collections;
import java.util.List;

public final class GameMapper {

    private GameMapper() {}

    public static GameResponse toResponse(Game game, List<GameScreenshot> screenshots,
                                          StorageService storageService,
                                          CoverImageService coverImageService) {
        List<ScreenshotResponse> screenshotResponses = screenshots == null
                ? Collections.emptyList()
                : screenshots.stream()
                        .map(s -> new ScreenshotResponse(
                                s.getId(),
                                storageService.getPublicUrl(s.getFilePath()),
                                s.getUploadedAt()))
                        .toList();

        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getPlatform(),
                game.getStatus(),
                game.getHoursPlayed(),
                game.getPersonalRating(),
                game.getExternalRating(),
                game.getNotes(),
                toDisplayCover(game.getCoverUrl(), coverImageService),
                game.getStartedAt(),
                game.getCompletedAt(),
                game.getCreatedAt(),
                screenshotResponses
        );
    }

    private static String toDisplayCover(String coverUrl, CoverImageService coverImageService) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (coverUrl.startsWith("/uploads") || coverUrl.contains("steamstatic.com")) {
            return coverUrl;
        }
        return coverImageService.toDisplayUrl(coverUrl);
    }

    public static GameResponse toResponse(Game game, StorageService storageService,
                                          CoverImageService coverImageService) {
        return toResponse(game, Collections.emptyList(), storageService, coverImageService);
    }
}

package com.memorycard.service;

import com.memorycard.dto.response.FriendGameDetailView;
import com.memorycard.dto.response.JournalEntryResponse;
import com.memorycard.dto.response.RetroAchievementProgress;
import com.memorycard.dto.response.ScreenshotResponse;
import com.memorycard.entity.Game;
import com.memorycard.entity.GameScreenshot;
import com.memorycard.entity.User;
import com.memorycard.exception.AccessDeniedException;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameRepository;
import com.memorycard.repository.GameScreenshotRepository;
import com.memorycard.repository.UserRepository;
import com.memorycard.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class FriendGameViewService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GameScreenshotRepository screenshotRepository;
    private final FriendService friendService;
    private final GameJournalService gameJournalService;
    private final RetroAchievementsService retroAchievementsService;
    private final StorageService storageService;
    private final CoverImageService coverImageService;

    public FriendGameViewService(UserRepository userRepository,
                                 GameRepository gameRepository,
                                 GameScreenshotRepository screenshotRepository,
                                 FriendService friendService,
                                 GameJournalService gameJournalService,
                                 RetroAchievementsService retroAchievementsService,
                                 StorageService storageService,
                                 CoverImageService coverImageService) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.screenshotRepository = screenshotRepository;
        this.friendService = friendService;
        this.gameJournalService = gameJournalService;
        this.retroAchievementsService = retroAchievementsService;
        this.storageService = storageService;
        this.coverImageService = coverImageService;
    }

    @Transactional(readOnly = true)
    public FriendGameDetailView getGameForViewer(Long viewerId, Long ownerId, Long gameId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        boolean ownProfile = viewerId.equals(ownerId);
        boolean friend = friendService.areFriends(viewerId, ownerId);
        if (!ownProfile && !friend) {
            throw new AccessDeniedException("Você precisa ser amigo deste usuário para ver o jogo");
        }

        Game game = gameRepository.findByIdAndUserId(gameId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));

        boolean notesVisible = ownProfile || owner.isShareNotesWithFriends();
        boolean journalVisible = ownProfile || owner.isShareJournalWithFriends();
        boolean screenshotsVisible = ownProfile || owner.isShareScreenshotsWithFriends();

        List<JournalEntryResponse> journal = journalVisible
                ? gameJournalService.findByGame(ownerId, gameId)
                : Collections.emptyList();

        List<ScreenshotResponse> screenshots = screenshotsVisible
                ? mapScreenshots(gameId)
                : Collections.emptyList();

        RetroAchievementProgress retroProgress = resolveRetroProgress(owner, game);

        return new FriendGameDetailView(
                game.getId(),
                owner.getId(),
                owner.getDisplayNick(),
                game.getTitle(),
                game.getPlatform(),
                game.getStatus(),
                game.getHoursPlayed(),
                game.getPersonalRating(),
                game.getExternalRating(),
                game.getRatingSource(),
                toDisplayCover(game.getCoverUrl()),
                game.getStartedAt(),
                game.getCompletedAt(),
                game.getCompletionType(),
                game.getTags(),
                game.isRetro(),
                game.getEmulator(),
                retroProgress,
                notesVisible ? game.getNotes() : null,
                journal,
                screenshots,
                notesVisible && game.getNotes() != null && !game.getNotes().isBlank(),
                journalVisible,
                screenshotsVisible
        );
    }

    private List<ScreenshotResponse> mapScreenshots(Long gameId) {
        List<GameScreenshot> screenshots = screenshotRepository.findByGameIdOrderByUploadedAtDesc(gameId);
        return screenshots.stream()
                .map(s -> new ScreenshotResponse(
                        s.getId(),
                        storageService.getPublicUrl(s.getFilePath()),
                        s.getUploadedAt()))
                .toList();
    }

    private RetroAchievementProgress resolveRetroProgress(User user, Game game) {
        if (!game.isRetro() || game.getRetroAchievementsGameId() == null) {
            return null;
        }
        RetroAchievementProgress progress = retroAchievementsService.fetchProgress(
                user, game.getRetroAchievementsGameId());
        if (progress != null) {
            return progress;
        }
        if (game.getRetroProgressPercent() != null) {
            return new RetroAchievementProgress(
                    game.getRetroAchievementsGameId(),
                    game.getTitle(),
                    game.getPlatform(),
                    game.getRetroProgressPercent(),
                    0,
                    0,
                    "https://retroachievements.org/game/" + game.getRetroAchievementsGameId(),
                    false
            );
        }
        return null;
    }

    private String toDisplayCover(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (coverUrl.startsWith("/uploads") || coverUrl.contains("steamstatic.com")) {
            return coverUrl;
        }
        return coverImageService.toDisplayUrl(coverUrl);
    }
}

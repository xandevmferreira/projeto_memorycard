package com.memorycard.service;

import com.memorycard.dto.response.DashboardResponse;
import com.memorycard.dto.response.DashboardStats;
import com.memorycard.dto.response.GameResponse;
import com.memorycard.dto.response.PopularGameResponse;
import com.memorycard.repository.GameRepository;
import com.memorycard.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final GameRepository gameRepository;
    private final StorageService storageService;
    private final GamingNewsService gamingNewsService;
    private final CoverImageService coverImageService;
    private final GameJournalService gameJournalService;
    private final CommunityService communityService;

    public DashboardService(GameRepository gameRepository,
                            StorageService storageService,
                            GamingNewsService gamingNewsService,
                            CoverImageService coverImageService,
                            GameJournalService gameJournalService,
                            CommunityService communityService) {
        this.gameRepository = gameRepository;
        this.storageService = storageService;
        this.gamingNewsService = gamingNewsService;
        this.coverImageService = coverImageService;
        this.gameJournalService = gameJournalService;
        this.communityService = communityService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        long totalGames = gameRepository.countByUserId(userId);
        long completedGames = gameRepository.countByUserIdAndStatus(userId, com.memorycard.entity.GameStatus.COMPLETED);
        BigDecimal totalHours = gameRepository.sumHoursPlayedByUserId(userId);

        DashboardStats stats = buildStats(userId, totalGames, completedGames);

        List<GameResponse> recentGames = gameRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(game -> GameMapper.toResponse(game, storageService, coverImageService))
                .toList();

        return new DashboardResponse(
                totalGames,
                completedGames,
                totalHours,
                stats,
                recentGames,
                getMockPopularGames(),
                gamingNewsService.getLatestNews(),
                communityService.recentCompletions(6),
                communityService.leaderboard(5)
        );
    }

    private DashboardStats buildStats(Long userId, long totalGames, long completedGames) {
        long retroGames = gameRepository.countByUserIdAndRetroTrue(userId);
        long completedThisYear = gameRepository.countCompletedSince(userId, LocalDate.of(LocalDate.now().getYear(), 1, 1));
        int completionRate = totalGames == 0
                ? 0
                : BigDecimal.valueOf(completedGames * 100.0 / totalGames).setScale(0, RoundingMode.HALF_UP).intValue();
        String topPlatform = gameRepository.countByPlatform(userId).stream()
                .findFirst()
                .map(row -> (String) row[0])
                .orElse("—");
        long journalEntries = gameJournalService.countByUser(userId);
        return new DashboardStats(retroGames, completedThisYear, completionRate, topPlatform, journalEntries);
    }

    private List<PopularGameResponse> getMockPopularGames() {
        return List.of(
                new PopularGameResponse("Elden Ring", "PC", BigDecimal.valueOf(9.5),
                        "https://media.rawg.io/media/games/5ec/5ecac5cb026ec26a56efcc546324e348.jpg",
                        LocalDate.of(2022, 2, 25), 1_250_000),
                new PopularGameResponse("Baldur's Gate 3", "PC", BigDecimal.valueOf(9.6),
                        "https://media.rawg.io/media/games/26c/26c44767c18f81f4f2e444e7ebd1d0f2.jpg",
                        LocalDate.of(2023, 8, 3), 980_000),
                new PopularGameResponse("Super Mario World", "SNES", BigDecimal.valueOf(9.2),
                        null, LocalDate.of(1990, 11, 21), 750_000),
                new PopularGameResponse("Final Fantasy III", "SNES", BigDecimal.valueOf(9.0),
                        null, LocalDate.of(1994, 4, 2), 620_000),
                new PopularGameResponse("Hades II", "PC", BigDecimal.valueOf(9.3),
                        null, LocalDate.of(2024, 5, 6), 540_000)
        );
    }
}

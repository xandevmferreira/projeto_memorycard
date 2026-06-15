package com.memorycard.service;

import com.memorycard.dto.response.DashboardResponse;
import com.memorycard.dto.response.GameResponse;
import com.memorycard.dto.response.PopularGameResponse;
import com.memorycard.entity.GameStatus;
import com.memorycard.repository.GameRepository;
import com.memorycard.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final GameRepository gameRepository;
    private final StorageService storageService;
    private final GamingNewsService gamingNewsService;
    private final CoverImageService coverImageService;

    public DashboardService(GameRepository gameRepository,
                            StorageService storageService,
                            GamingNewsService gamingNewsService,
                            CoverImageService coverImageService) {
        this.gameRepository = gameRepository;
        this.storageService = storageService;
        this.gamingNewsService = gamingNewsService;
        this.coverImageService = coverImageService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        long totalGames = gameRepository.countByUserId(userId);
        long completedGames = gameRepository.countByUserIdAndStatus(userId, GameStatus.COMPLETED);
        BigDecimal totalHours = gameRepository.sumHoursPlayedByUserId(userId);

        List<GameResponse> recentGames = gameRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(game -> GameMapper.toResponse(game, storageService, coverImageService))
                .toList();

        List<PopularGameResponse> popularGames = getMockPopularGames();
        List<com.memorycard.dto.response.GamingNewsItem> news = gamingNewsService.getLatestNews();

        return new DashboardResponse(totalGames, completedGames, totalHours, recentGames, popularGames, news);
    }

    private List<PopularGameResponse> getMockPopularGames() {
        return List.of(
                new PopularGameResponse("Elden Ring", "PC", BigDecimal.valueOf(9.5),
                        "https://media.rawg.io/media/games/5ec/5ecac5cb026ec26a56efcc546324e348.jpg",
                        LocalDate.of(2022, 2, 25), 1_250_000),
                new PopularGameResponse("Baldur's Gate 3", "PC", BigDecimal.valueOf(9.6),
                        "https://media.rawg.io/media/games/26c/26c44767c18f81f4f2e444e7ebd1d0f2.jpg",
                        LocalDate.of(2023, 8, 3), 980_000),
                new PopularGameResponse("Hollow Knight: Silksong", "Nintendo Switch", BigDecimal.valueOf(9.2),
                        null, LocalDate.of(2025, 9, 4), 750_000),
                new PopularGameResponse("Clair Obscur: Expedition 33", "PlayStation 5", BigDecimal.valueOf(9.0),
                        null, LocalDate.of(2025, 4, 24), 620_000),
                new PopularGameResponse("Hades II", "PC", BigDecimal.valueOf(9.3),
                        null, LocalDate.of(2024, 5, 6), 540_000)
        );
    }
}

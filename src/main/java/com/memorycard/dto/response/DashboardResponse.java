package com.memorycard.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalGames,
        long completedGames,
        BigDecimal totalHoursPlayed,
        List<GameResponse> recentGames,
        List<PopularGameResponse> popularGamesThisWeek,
        List<GamingNewsItem> gamingNews
) {}

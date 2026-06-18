package com.memorycard.dto.response;

import com.memorycard.entity.GameStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UserPublicProfile(
        Long id,
        String name,
        Instant memberSince,
        long totalGames,
        long completedGames,
        BigDecimal totalHours,
        List<GameSummaryView> games,
        List<BadgeView> badges,
        boolean ownProfile,
        boolean friend
) {}

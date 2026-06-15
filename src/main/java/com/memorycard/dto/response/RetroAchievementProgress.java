package com.memorycard.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RetroAchievementProgress(
        int gameId,
        String title,
        String consoleName,
        BigDecimal progressPercent,
        int achievementsEarned,
        int achievementsTotal,
        String profileUrl,
        boolean fromApi
) {}

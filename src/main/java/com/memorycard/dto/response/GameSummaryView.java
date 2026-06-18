package com.memorycard.dto.response;

import com.memorycard.entity.GameStatus;

public record GameSummaryView(
        Long id,
        String title,
        String platform,
        GameStatus status,
        String coverUrl,
        java.math.BigDecimal metacriticScore,
        java.math.BigDecimal personalRating
) {}

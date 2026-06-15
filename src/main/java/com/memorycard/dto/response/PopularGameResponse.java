package com.memorycard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PopularGameResponse(
        String title,
        String platform,
        BigDecimal externalRating,
        String coverUrl,
        LocalDate releaseDate,
        long weeklyPlayers
) {}

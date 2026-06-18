package com.memorycard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExternalGameInfo(
        String title,
        String platform,
        BigDecimal externalRating,
        String ratingSource,
        String coverUrl,
        LocalDate releaseDate,
        List<String> platforms
) {}

package com.memorycard.dto.response;

import java.time.Instant;
import java.util.List;

public record GameListResponse(
        Long id,
        String name,
        String description,
        int gameCount,
        Instant createdAt,
        List<GameResponse> games
) {}

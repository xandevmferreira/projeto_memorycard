package com.memorycard.dto.response;

import java.time.Instant;

public record PlaySessionView(
        Long id,
        Long gameId,
        String gameTitle,
        Instant startedAt,
        String source,
        String processName
) {}

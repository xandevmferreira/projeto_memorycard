package com.memorycard.dto.response;

import java.time.Instant;

public record JournalEntryResponse(
        Long id,
        String content,
        boolean spoiler,
        Instant createdAt
) {}

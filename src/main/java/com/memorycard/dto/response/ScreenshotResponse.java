package com.memorycard.dto.response;

import java.time.Instant;

public record ScreenshotResponse(
        Long id,
        String url,
        Instant uploadedAt
) {}

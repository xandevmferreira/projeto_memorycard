package com.memorycard.dto.response;

import java.time.Instant;

public record BadgeView(
        String code,
        String name,
        String description,
        String icon,
        boolean earned,
        Instant earnedAt
) {}

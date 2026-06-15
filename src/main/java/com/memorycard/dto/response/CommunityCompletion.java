package com.memorycard.dto.response;

import java.time.LocalDate;

public record CommunityCompletion(
        String playerName,
        String gameTitle,
        String platform,
        LocalDate completedAt,
        String completionLabel
) {}

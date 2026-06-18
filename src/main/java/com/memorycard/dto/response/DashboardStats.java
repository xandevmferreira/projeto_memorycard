package com.memorycard.dto.response;

import java.time.LocalDate;

public record DashboardStats(
        long retroGames,
        long completedThisYear,
        int completionRatePercent,
        String topPlatform,
        long journalEntries
) {}

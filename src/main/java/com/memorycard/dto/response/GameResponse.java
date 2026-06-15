package com.memorycard.dto.response;

import com.memorycard.entity.CompletionType;
import com.memorycard.entity.GameStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GameResponse(
        Long id,
        String title,
        String platform,
        GameStatus status,
        BigDecimal hoursPlayed,
        BigDecimal personalRating,
        BigDecimal externalRating,
        String notes,
        String coverUrl,
        LocalDate startedAt,
        LocalDate completedAt,
        CompletionType completionType,
        String tags,
        boolean retro,
        String emulator,
        Integer retroAchievementsGameId,
        Integer retroConsoleId,
        BigDecimal retroProgressPercent,
        RetroAchievementProgress retroProgress,
        Instant createdAt,
        List<ScreenshotResponse> screenshots,
        List<JournalEntryResponse> journalEntries
) {}

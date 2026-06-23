package com.memorycard.dto.response;

import com.memorycard.entity.CompletionType;
import com.memorycard.entity.GameStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FriendGameDetailView(
        Long id,
        Long ownerId,
        String ownerName,
        String title,
        String platform,
        GameStatus status,
        BigDecimal hoursPlayed,
        BigDecimal personalRating,
        BigDecimal externalRating,
        String ratingSource,
        String coverUrl,
        LocalDate startedAt,
        LocalDate completedAt,
        CompletionType completionType,
        String tags,
        boolean retro,
        String emulator,
        RetroAchievementProgress retroProgress,
        String notes,
        List<JournalEntryResponse> journalEntries,
        List<ScreenshotResponse> screenshots,
        boolean notesVisible,
        boolean journalVisible,
        boolean screenshotsVisible
) {}

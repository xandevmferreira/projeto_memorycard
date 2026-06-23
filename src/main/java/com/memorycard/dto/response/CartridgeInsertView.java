package com.memorycard.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record CartridgeInsertView(
        Long gameId,
        String gameTitle,
        String gameCoverUrl,
        String platform,
        boolean canPlayInBrowser,
        String emulatorCore,
        String romExtensionsHint,
        Long cartridgeId,
        String label,
        String memories,
        LocalDate sessionDate,
        Instant createdAt,
        String emulatorHint,
        String snapshotUrl,
        Long stateFileId,
        Long saveFileId,
        String statePreviewUrl,
        boolean hasState,
        boolean hasSave
) {}

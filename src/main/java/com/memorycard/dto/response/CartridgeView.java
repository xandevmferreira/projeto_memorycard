package com.memorycard.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CartridgeView(
        Long id,
        String label,
        String memories,
        LocalDate sessionDate,
        String emulatorHint,
        Instant createdAt,
        String snapshotUrl,
        boolean hasState,
        boolean hasSave,
        boolean canPlayInBrowser,
        List<ArchiveFileView> files
) {}

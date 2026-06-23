package com.memorycard.dto.response;

import com.memorycard.entity.ArchiveFileType;
import java.time.Instant;

public record ArchiveFileView(
        Long id,
        ArchiveFileType fileType,
        String originalFilename,
        String relativePath,
        long fileSize,
        Instant uploadedAt
) {}

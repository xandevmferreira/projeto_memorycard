package com.memorycard.sync.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchiveFileInfo(
        long id,
        String fileType,
        String originalFilename,
        String relativePath,
        long fileSize
) {}

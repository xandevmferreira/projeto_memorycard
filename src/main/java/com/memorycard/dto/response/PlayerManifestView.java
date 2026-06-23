package com.memorycard.dto.response;

public record PlayerManifestView(
        Long gameId,
        Long cartridgeId,
        String gameTitle,
        String platform,
        String emulatorCore,
        String romExtensionsHint,
        Long stateFileId,
        Long saveFileId,
        String stateDownloadUrl,
        String saveDownloadUrl,
        boolean hasState,
        boolean hasSave
) {}

package com.memorycard.dto.response;

import java.util.List;

public record RestoreManifestView(
        Long cartridgeId,
        String cartridgeLabel,
        List<ArchiveFileView> files
) {}

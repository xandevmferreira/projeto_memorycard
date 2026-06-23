package com.memorycard.sync.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestoreManifest(
        Long cartridgeId,
        String cartridgeLabel,
        List<ArchiveFileInfo> files
) {
    public boolean hasFiles() {
        return files != null && !files.isEmpty();
    }
}

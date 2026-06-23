package com.memorycard.entity;

public enum ArchiveFileType {
    SAVE("Save"),
    ROM("ROM"),
    STATE("Estado do emulador");

    private final String label;

    ArchiveFileType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

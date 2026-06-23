package com.memorycard.sync.watcher;

import java.util.Locale;
import java.util.Set;

public final class SaveFilePatterns {

    private SaveFilePatterns() {}

    public static final Set<String> SAVE_EXTENSIONS = Set.of(
            ".sav", ".srm", ".sra", ".sta", ".dsv", ".mcr", ".mem", ".eep",
            ".state", ".st0", ".st1", ".st2", ".savestate",
            ".save", ".dat", ".json", ".bin", ".xml", ".ini", ".cfg", ".profile"
    );

    /** Extensões típicas de emulador — não inclui .dat genérico (evita falso positivo). */
    public static final Set<String> EMULATOR_SAVE_EXTENSIONS = Set.of(
            ".sav", ".srm", ".sra", ".sta", ".dsv", ".mcr", ".mem", ".eep",
            ".save", ".oops", ".ss0", ".ss1", ".ss2",
            ".state", ".st0", ".st1", ".st2", ".savestate"
    );

    public static final Set<String> SAVE_DIR_KEYWORDS = Set.of(
            "savegames", "savegame", "saved", "saves", "save", "save data",
            "save games", "wgs", "userdata", "steamuserdata"
    );

    public static boolean isSaveExtension(String filename) {
        String ext = extension(filename);
        return !ext.isEmpty() && SAVE_EXTENSIONS.contains(ext);
    }

    public static boolean isSaveDirectoryName(String dirName) {
        if (dirName == null || dirName.isBlank()) {
            return false;
        }
        String normalized = normalize(dirName);
        for (String keyword : SAVE_DIR_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** Arquivo em pasta de save — aceita sem extensão conhecida (ex.: Xbox wgs). */
    public static boolean isLikelySaveFile(PathContext ctx) {
        if (ctx.fileName().startsWith(".")) {
            return false;
        }
        if (isSaveExtension(ctx.fileName())) {
            return true;
        }
        return ctx.inSaveDirectory() && !ctx.fileName().endsWith(".log");
    }

    public static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i).toLowerCase(Locale.ROOT);
    }

    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    public record PathContext(String fileName, boolean inSaveDirectory) {}
}

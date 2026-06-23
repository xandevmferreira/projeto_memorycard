package com.memorycard.sync.watcher;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class GameTitleMatch {

    private GameTitleMatch() {}

    public static boolean pathMatchesGame(Path folder, String gameTitle, List<String> tokens) {
        if (folder == null || gameTitle == null || gameTitle.isBlank()) {
            return false;
        }
        if (BlockedSavePaths.isBlocked(folder)) {
            return false;
        }
        if (RetroArchSaveLocator.hasGameSaveInFolder(folder, gameTitle, tokens)) {
            return true;
        }
        if (!titleAppearsInPath(folder, gameTitle, tokens)) {
            return false;
        }
        return looksLikeSaveLocation(folder, gameTitle, tokens);
    }

    private static boolean titleAppearsInPath(Path folder, String gameTitle, List<String> tokens) {
        String normTitle = SaveFilePatterns.normalize(gameTitle);
        String compact = normTitle.replace(" ", "");

        for (Path p = folder; p != null; p = p.getParent()) {
            if (p.getFileName() == null) {
                continue;
            }
            if (segmentMatchesTitle(p.getFileName().toString(), tokens, normTitle, compact)) {
                return true;
            }
        }

        String pathNorm = SaveFilePatterns.normalize(folder.toString()).replace(" ", "");
        return compact.length() >= 3 && pathNorm.contains(compact);
    }

    private static boolean looksLikeSaveLocation(Path folder, String gameTitle, List<String> tokens) {
        String path = folder.toString().toLowerCase(Locale.ROOT).replace('\\', '/');

        if (path.contains("xboxgames") && !path.contains("savegames")
                && !path.contains("/saved/") && !path.contains("/wgs")) {
            return false;
        }
        if (path.contains("windowsapps")) {
            return false;
        }

        if (path.contains("microsoftstore") || path.contains("/packages/")) {
            return path.contains("savegames") || path.contains("/wgs")
                    || path.contains("/saved/") || path.contains("saved games");
        }

        if (RetroArchSaveLocator.isRetroArchWatchFolder(folder)) {
            return RetroArchSaveLocator.hasGameSaveInFolder(folder, gameTitle, tokens)
                    || ProcessScanner.isRetroArchRunning();
        }

        for (Path p = folder; p != null; p = p.getParent()) {
            if (p.getFileName() != null
                    && SaveFilePatterns.isSaveDirectoryName(p.getFileName().toString())) {
                return !BlockedSavePaths.isBlocked(p);
            }
        }
        return SaveFolderDetector.countSaveFilesIn(folder) > 0;
    }

    private static boolean segmentMatchesTitle(String name, List<String> tokens,
                                               String normTitle, String compact) {
        String norm = SaveFilePatterns.normalize(name);
        if (norm.isEmpty()) {
            return false;
        }
        String normCompact = norm.replace(" ", "");
        if (compact.length() >= 3 && (normCompact.contains(compact) || compact.contains(normCompact))) {
            return true;
        }
        if (norm.contains(normTitle) || normTitle.contains(norm)) {
            return true;
        }
        int hits = 0;
        for (String token : tokens) {
            if (token.length() >= 3 && norm.contains(token)) {
                hits++;
            }
        }
        return hits >= Math.min(2, tokens.size()) || (tokens.size() == 1 && hits == 1);
    }
}

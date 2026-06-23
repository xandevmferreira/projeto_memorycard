package com.memorycard.sync.watcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GameTitleTokens {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "of", "and", "for", "game", "edition", "definitive", "deluxe"
    );

    private GameTitleTokens() {}

    public static List<String> fromTitle(String title) {
        String norm = SaveFilePatterns.normalize(title);
        if (norm.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : norm.split("\\s+")) {
            if (part.length() >= 3 && !STOP_WORDS.contains(part)) {
                tokens.add(part);
            }
        }
        if (tokens.isEmpty()) {
            tokens.add(norm.replace(" ", ""));
        }
        return tokens;
    }

    public static boolean filenameMatches(String filename, String gameTitle) {
        if (filename == null || gameTitle == null || gameTitle.isBlank()) {
            return false;
        }
        List<String> tokens = fromTitle(gameTitle);
        if (tokens.isEmpty()) {
            return false;
        }
        String normFile = SaveFilePatterns.normalize(filename);
        String compactTitle = SaveFilePatterns.normalize(gameTitle).replace(" ", "");
        if (compactTitle.length() >= 3 && normFile.replace(" ", "").contains(compactTitle)) {
            return true;
        }
        int hits = 0;
        for (String token : tokens) {
            if (normFile.contains(token)) {
                hits++;
            }
        }
        return hits >= Math.min(2, tokens.size()) || (tokens.size() == 1 && hits == 1);
    }
}

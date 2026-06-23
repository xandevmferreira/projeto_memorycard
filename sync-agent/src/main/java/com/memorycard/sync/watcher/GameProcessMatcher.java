package com.memorycard.sync.watcher;

import com.memorycard.sync.api.GameSummary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GameProcessMatcher {

    private GameProcessMatcher() {}

    public static Optional<GameSummary> matchRunningGame(List<GameSummary> games,
                                                         List<ProcessScanner.RunningProcess> processes,
                                                         Map<Long, String> configuredProcessNames) {
        for (GameSummary game : games) {
            String configured = configuredProcessNames.get(game.id());
            if (configured != null && !configured.isBlank()) {
                if (processes.stream().anyMatch(p -> processNameMatches(p, configured))) {
                    return Optional.of(game);
                }
                continue;
            }
            if (matchesByTitle(game, processes)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    private static boolean matchesByTitle(GameSummary game, List<ProcessScanner.RunningProcess> processes) {
        String compactTitle = SaveFilePatterns.normalize(game.title()).replace(" ", "");
        if (compactTitle.length() < 3) {
            return false;
        }
        for (ProcessScanner.RunningProcess process : processes) {
            String procCompact = ProcessScanner.normalizeProcessName(process.name());
            if (!procCompact.isEmpty() && (procCompact.contains(compactTitle) || compactTitle.contains(procCompact))) {
                return true;
            }
            String cmdNorm = SaveFilePatterns.normalize(process.commandLine()).replace(" ", "");
            if (cmdNorm.contains(compactTitle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean processNameMatches(ProcessScanner.RunningProcess process, String configured) {
        String want = ProcessScanner.normalizeProcessName(configured);
        String have = ProcessScanner.normalizeProcessName(process.name());
        return !want.isEmpty() && (have.equals(want) || have.contains(want) || want.contains(have));
    }

    public static String guessProcessName(GameSummary game, List<ProcessScanner.RunningProcess> processes) {
        String compactTitle = SaveFilePatterns.normalize(game.title()).replace(" ", "");
        for (ProcessScanner.RunningProcess process : processes) {
            String procCompact = ProcessScanner.normalizeProcessName(process.name());
            if (!procCompact.isEmpty() && (procCompact.contains(compactTitle) || compactTitle.contains(procCompact))) {
                return process.name().replace(".exe", "").replace(".EXE", "");
            }
            String cmdNorm = SaveFilePatterns.normalize(process.commandLine()).replace(" ", "");
            if (cmdNorm.contains(compactTitle)) {
                return process.name().replace(".exe", "").replace(".EXE", "");
            }
        }
        return "";
    }
}

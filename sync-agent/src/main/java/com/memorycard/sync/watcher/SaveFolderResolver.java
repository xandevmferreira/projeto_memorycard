package com.memorycard.sync.watcher;

import com.memorycard.sync.api.GameSummary;
import com.memorycard.sync.config.AgentConfig;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Resolve a pasta de save de um jogo (cache, detecção, RetroArch). */
public final class SaveFolderResolver {

    private SaveFolderResolver() {}

    public static Optional<Path> resolve(GameSummary game, AgentConfig config) {
        return resolve(game.id(), game.title(), config);
    }

    public static Optional<Path> resolve(long gameId, String gameTitle, AgentConfig config) {
        if (gameTitle == null || gameTitle.isBlank()) {
            return Optional.empty();
        }

        String cached = config.getSaveFolder(gameId);
        if (cached != null && !cached.isBlank()) {
            Path path = Path.of(cached);
            if (SaveFolderValidator.canWatch(path, gameTitle)) {
                return Optional.of(path);
            }
            config.setSaveFolder(gameId, "");
        }

        Optional<SaveFolderDetector.Result> detected = new SaveFolderDetector().detect(gameTitle);
        if (detected.isPresent()) {
            Path folder = detected.get().folder();
            config.setSaveFolder(gameId, folder.toString());
            return Optional.of(folder);
        }

        List<String> tokens = GameTitleTokens.fromTitle(gameTitle);
        Optional<Path> retro = RetroArchSaveLocator.resolveWatchFolder(gameTitle, tokens);
        if (retro.isPresent()) {
            config.setSaveFolder(gameId, retro.get().toString());
        }
        return retro;
    }

    public static String describeFailure(String gameTitle) {
        List<String> tokens = GameTitleTokens.fromTitle(gameTitle);
        Set<String> hints = new HashSet<>();
        if (RetroArchSaveLocator.primarySaveRoot().isEmpty()) {
            hints.add("RetroArch: instale ou salve in-game (Settings → Directory → Save Files)");
        } else {
            hints.add("RetroArch: salve in-game para criar o arquivo .srm");
        }
        hints.add("Xbox/Store: salve no jogo e deixe este programa aberto");
        if (tokens.isEmpty()) {
            return String.join("; ", hints);
        }
        return "Sem pasta de save — " + String.join("; ", hints);
    }
}

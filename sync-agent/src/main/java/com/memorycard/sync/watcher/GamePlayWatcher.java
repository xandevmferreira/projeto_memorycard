package com.memorycard.sync.watcher;

import com.memorycard.sync.api.GameSummary;
import com.memorycard.sync.api.MemoryCardClient;
import com.memorycard.sync.config.AgentConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modo automático: detecta jogo rodando, restaura saves do site e monitora a pasta até fechar.
 */
public class GamePlayWatcher implements Runnable {

    private final MemoryCardClient client;
    private final List<GameSummary> games;
    private final AgentConfig config;
    private final Consumer<String> log;
    private final Runnable onUnauthorized;

    private volatile boolean running = true;
    private Long activeGameId;
    private String activeGameTitle;
    private Path activeSaveFolder;
    private FolderSyncWorker syncWorker;
    private Thread syncThread;
    private final Map<Long, Path> saveFolderCache = new HashMap<>();

    public GamePlayWatcher(MemoryCardClient client,
                           List<GameSummary> games,
                           AgentConfig config,
                           Consumer<String> log,
                           Runnable onUnauthorized) {
        this.client = client;
        this.games = games;
        this.config = config;
        this.log = log;
        this.onUnauthorized = onUnauthorized;
    }

    public void stop() {
        running = false;
        stopSyncWorker();
    }

    @Override
    public void run() {
        log.accept("Modo automático ativo — monitorando " + games.size() + " jogo(s).");
        log.accept("Abra um jogo da sua biblioteca; saves serão restaurados e enviados ao fechar.");

        while (running) {
            try {
                tick();
            } catch (Exception e) {
                log.accept("Erro no modo automático: " + e.getMessage());
            }
            sleep(config.getPollSeconds());
        }
        stopSyncWorker();
        log.accept("Modo automático encerrado.");
    }

    private void tick() throws IOException, InterruptedException {
        List<ProcessScanner.RunningProcess> processes = ProcessScanner.listRunning();
        Optional<GameSummary> playing = GameProcessMatcher.matchRunningGame(
                games, processes, config.getProcessNamesByGameId());

        if (playing.isPresent()) {
            GameSummary game = playing.get();
            if (activeGameId == null || activeGameId != game.id()) {
                onGameStarted(game);
            }
            return;
        }

        if (activeGameId != null) {
            onGameStopped();
        }
    }

    private void onGameStarted(GameSummary game) throws IOException, InterruptedException {
        stopSyncWorker();
        activeGameId = game.id();
        activeGameTitle = game.title();
        log.accept("Jogo detectado: " + game.title());

        activeSaveFolder = resolveSaveFolder(game);
        if (activeSaveFolder == null) {
            log.accept("Pasta de save não encontrada para \"" + game.title() + "\".");
            log.accept("Dica: salve no jogo uma vez, ou abra o RetroArch e confira Settings → Directory → Save Files.");
            activeGameId = null;
            activeGameTitle = null;
            return;
        }

        log.accept("Pasta de save: " + activeSaveFolder);
        config.setSaveFolder(game.id(), activeSaveFolder.toString());
        config.setWatchFolder(activeSaveFolder.toString());
        config.setGameId(game.id());
        try {
            config.save();
        } catch (IOException ignored) {
        }

        SaveRestorer restorer = new SaveRestorer(client, log);
        int count = restorer.restoreLatest(game.id(), activeSaveFolder);
        if (count > 0) {
            log.accept(count + " arquivo(s) restaurado(s) — pode carregar no jogo.");
        }

        startSyncWorker(game.id(), activeSaveFolder);
    }

    private void onGameStopped() {
        log.accept("Jogo encerrado: " + activeGameTitle + " — enviando saves finais...");
        stopSyncWorker();
        activeGameId = null;
        activeGameTitle = null;
        activeSaveFolder = null;
        log.accept("Pronto. Aguardando próximo jogo...");
    }

    private Path resolveSaveFolder(GameSummary game) {
        String saved = config.getSaveFolder(game.id());
        if (saved != null && !saved.isBlank()) {
            Path path = Path.of(saved);
            SaveFolderValidator.Check check = SaveFolderValidator.validate(path);
            if (check.status() == SaveFolderValidator.Status.OK) {
                return path;
            }
        }

        Path cached = saveFolderCache.get(game.id());
        if (cached != null) {
            SaveFolderValidator.Check check = SaveFolderValidator.validate(cached);
            if (check.status() == SaveFolderValidator.Status.OK) {
                return cached;
            }
        }

        Optional<SaveFolderDetector.Result> detected = new SaveFolderDetector().detect(game.title());
        if (detected.isPresent()) {
            Path folder = detected.get().folder();
            saveFolderCache.put(game.id(), folder);
            config.setSaveFolder(game.id(), folder.toString());
            try {
                config.save();
            } catch (IOException ignored) {
            }
            return folder;
        }
        return null;
    }

    private void startSyncWorker(long gameId, Path folder) {
        String title = activeGameTitle != null ? activeGameTitle : "";
        syncWorker = new FolderSyncWorker(
                client,
                gameId,
                title,
                folder,
                config.getPollSeconds(),
                log,
                onUnauthorized,
                () -> log.accept("Pasta sem saves durante o jogo — sync pausado.")
        );
        syncThread = new Thread(syncWorker, "memorycard-sync-" + gameId);
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void stopSyncWorker() {
        if (syncWorker != null) {
            syncWorker.flush();
            syncWorker.stop();
            syncWorker = null;
        }
        if (syncThread != null) {
            syncThread.interrupt();
            syncThread = null;
        }
    }

    private void sleep(int seconds) {
        for (int i = 0; i < seconds * 10 && running; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
}

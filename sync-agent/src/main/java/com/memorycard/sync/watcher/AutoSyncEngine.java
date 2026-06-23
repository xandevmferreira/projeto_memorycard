package com.memorycard.sync.watcher;

import com.memorycard.sync.api.GameSummary;
import com.memorycard.sync.api.MemoryCardClient;
import com.memorycard.sync.config.AgentConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Monitora todos os jogos da biblioteca e envia saves ao site automaticamente.
 */
public class AutoSyncEngine implements Runnable {

    private final MemoryCardClient client;
    private final AgentConfig config;
    private final Consumer<String> log;
    private final Runnable onUnauthorized;
    private final Supplier<List<GameSummary>> gameListSupplier;

    private volatile boolean running = true;
    private final Map<Long, GameWorker> workers = new ConcurrentHashMap<>();
    private final Set<Long> loggedMissing = ConcurrentHashMap.newKeySet();
    private int scanCycle;

    @FunctionalInterface
    public interface Supplier<T> {
        T get();
    }

    public AutoSyncEngine(MemoryCardClient client,
                          AgentConfig config,
                          Supplier<List<GameSummary>> gameListSupplier,
                          Consumer<String> log,
                          Runnable onUnauthorized) {
        this.client = client;
        this.config = config;
        this.gameListSupplier = gameListSupplier;
        this.log = log;
        this.onUnauthorized = onUnauthorized;
    }

    public void stop() {
        running = false;
        for (GameWorker worker : workers.values()) {
            worker.stop();
        }
        workers.clear();
    }

    public int activeWorkerCount() {
        return workers.size();
    }

    @Override
    public void run() {
        log.accept("Sync automático ligado — não precisa clicar em nada.");
        log.accept("Jogue e salve; os arquivos vão para o site sozinhos.");

        while (running) {
            try {
                tick();
            } catch (Exception e) {
                log.accept("Erro: " + e.getMessage());
            }
            sleep(Math.max(5, config.getPollSeconds()));
        }
        stop();
        log.accept("Sync parado.");
    }

    private void tick() throws IOException, InterruptedException {
        List<GameSummary> games = gameListSupplier.get();
        if (games == null || games.isEmpty()) {
            if (scanCycle == 0) {
                log.accept("Nenhum jogo na biblioteca — cadastre jogos no site primeiro.");
            }
            scanCycle++;
            return;
        }

        if (scanCycle == 0) {
            log.accept("Biblioteca: " + games.size() + " jogo(s). Procurando pastas de save...");
        }

        scanCycle++;
        Map<Long, Boolean> seen = new HashMap<>();

        for (GameSummary game : games) {
            seen.put(game.id(), true);
            ensureWorker(game);
        }

        workers.keySet().removeIf(id -> {
            if (seen.containsKey(id)) {
                return false;
            }
            GameWorker removed = workers.remove(id);
            if (removed != null) {
                removed.stop();
            }
            return true;
        });

        if (scanCycle == 1 || scanCycle % 4 == 0) {
            int active = workers.size();
            if (active == 0) {
                log.accept("Nenhum save encontrado ainda — salve no jogo e aguarde ~"
                        + config.getPollSeconds() + "s.");
            } else {
                log.accept("Monitorando " + active + " de " + games.size() + " jogo(s).");
            }
        }
    }

    private void ensureWorker(GameSummary game) throws IOException, InterruptedException {
        Optional<Path> folder = SaveFolderResolver.resolve(game, config);
        if (folder.isEmpty()) {
            if (loggedMissing.add(game.id())) {
                log.accept("✗ " + game.title() + " — " + SaveFolderResolver.describeFailure(game.title()));
            }
            GameWorker existing = workers.remove(game.id());
            if (existing != null) {
                existing.stop();
            }
            return;
        }

        loggedMissing.remove(game.id());
        Path savePath = folder.get();

        try {
            config.save();
        } catch (IOException ignored) {
        }

        GameWorker existing = workers.get(game.id());
        if (existing != null && existing.sameFolder(savePath)) {
            return;
        }

        if (existing != null) {
            existing.stop();
        }

        log.accept("✓ " + game.title() + " → " + savePath);

        SaveRestorer restorer = new SaveRestorer(client, log);
        int restored = restorer.restoreLatest(game.id(), savePath);
        if (restored > 0) {
            log.accept("  Restaurado do site: " + restored + " arquivo(s)");
        }

        List<Path> watchFolders = new ArrayList<>();
        watchFolders.add(savePath);
        RetroArchSaveLocator.companionWatchFolder(savePath).ifPresent(companion -> {
            if (!companion.equals(savePath)) {
                watchFolders.add(companion);
            }
        });

        List<FolderSyncWorker> syncWorkers = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (Path watchFolder : watchFolders) {
            FolderSyncWorker syncWorker = new FolderSyncWorker(
                    client,
                    game.id(),
                    game.title(),
                    watchFolder,
                    config.getPollSeconds(),
                    log,
                    onUnauthorized,
                    () -> { },
                    true
            );
            Thread thread = new Thread(syncWorker, "sync-game-" + game.id() + "-" + watchFolder.getFileName());
            thread.setDaemon(true);
            thread.start();
            syncWorkers.add(syncWorker);
            threads.add(thread);
            if (watchFolders.size() > 1) {
                log.accept("  Também monitorando: " + watchFolder);
            }
        }

        workers.put(game.id(), new GameWorker(savePath, watchFolders, syncWorkers, threads));
    }

    /** Força nova busca de pastas (ex.: após opções avançadas). */
    public void refreshAll() {
        loggedMissing.clear();
        for (GameWorker w : workers.values()) {
            w.stop();
        }
        workers.clear();
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

    private static final class GameWorker {
        private final Path primaryFolder;
        private final List<Path> watchFolders;
        private final List<FolderSyncWorker> syncWorkers;
        private final List<Thread> threads;

        GameWorker(Path primaryFolder, List<Path> watchFolders,
                   List<FolderSyncWorker> syncWorkers, List<Thread> threads) {
            this.primaryFolder = primaryFolder;
            this.watchFolders = watchFolders;
            this.syncWorkers = syncWorkers;
            this.threads = threads;
        }

        boolean sameFolder(Path other) {
            if (!primaryFolder.toAbsolutePath().normalize().equals(other.toAbsolutePath().normalize())) {
                return false;
            }
            List<Path> expected = new ArrayList<>();
            expected.add(other);
            RetroArchSaveLocator.companionWatchFolder(other).ifPresent(expected::add);
            if (expected.size() != watchFolders.size()) {
                return false;
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!expected.get(i).toAbsolutePath().normalize()
                        .equals(watchFolders.get(i).toAbsolutePath().normalize())) {
                    return false;
                }
            }
            return true;
        }

        void stop() {
            for (FolderSyncWorker worker : syncWorkers) {
                worker.flush();
                worker.stop();
            }
            for (Thread thread : threads) {
                thread.interrupt();
            }
        }
    }
}

package com.memorycard.sync.watcher;

import com.memorycard.sync.api.MemoryCardClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FolderSyncWorker implements Runnable {

    private static final int SCAN_DEPTH = 4;
    private static final long MAX_FILE_BYTES = 64L * 1024 * 1024;

    private final MemoryCardClient client;
    private final long gameId;
    private final String gameTitle;
    private final Path watchFolder;
    private final int pollSeconds;
    private final boolean sharedEmulatorFolder;
    private final Consumer<String> log;
    private final Runnable onUnauthorized;
    private final Runnable onNoSaveFiles;
    private final boolean keepWatchingWhenEmpty;

    private volatile boolean running = true;
    private final Map<String, String> known = new HashMap<>();
    private int pollCount;

    public FolderSyncWorker(MemoryCardClient client,
                            long gameId,
                            String gameTitle,
                            Path watchFolder,
                            int pollSeconds,
                            Consumer<String> log,
                            Runnable onUnauthorized,
                            Runnable onNoSaveFiles) {
        this(client, gameId, gameTitle, watchFolder, pollSeconds, log, onUnauthorized, onNoSaveFiles, false);
    }

    public FolderSyncWorker(MemoryCardClient client,
                            long gameId,
                            String gameTitle,
                            Path watchFolder,
                            int pollSeconds,
                            Consumer<String> log,
                            Runnable onUnauthorized,
                            Runnable onNoSaveFiles,
                            boolean keepWatchingWhenEmpty) {
        this.client = client;
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.watchFolder = watchFolder;
        this.pollSeconds = pollSeconds;
        this.sharedEmulatorFolder = SaveFolderDetector.isSharedEmulatorFolder(watchFolder);
        this.log = log;
        this.onUnauthorized = onUnauthorized;
        this.onNoSaveFiles = onNoSaveFiles;
        this.keepWatchingWhenEmpty = keepWatchingWhenEmpty;
    }

    public void stop() {
        running = false;
    }

    /** Envia alterações pendentes antes de encerrar. */
    public void flush() {
        try {
            scanOnce();
        } catch (Exception e) {
            log.accept("Erro ao enviar saves finais: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        log.accept("Monitorando: " + watchFolder);
        List<Path> initial = listSaveFiles();
        log.accept(initial.size() + " arquivo(s) de save nesta pasta.");
        if (initial.isEmpty()) {
            if (keepWatchingWhenEmpty) {
                log.accept("Aguardando saves de " + gameTitle + " nesta pasta...");
            } else {
                log.accept("Nenhum save aqui — esta pasta não serve. Use Detectar saves.");
                onNoSaveFiles.run();
                return;
            }
        } else {
            log.accept("Enviando " + initial.size() + " save(s) para o site...");
            for (Path file : initial) {
                handleFile(file);
            }
        }
        log.accept("Aguardando alterações nos saves...");

        while (running) {
            try {
                scanOnce();
            } catch (Exception e) {
                log.accept("Erro: " + e.getMessage());
            }
            pollCount++;
            if (pollCount % 4 == 0) {
                log.accept("Ativo — " + listSaveFiles().size() + " save(s) monitorado(s).");
            }
            sleep(pollSeconds);
        }
        log.accept("Sync parado.");
    }

    private void scanOnce() throws IOException {
        if (!Files.isDirectory(watchFolder)) {
            throw new IOException("Pasta não encontrada: " + watchFolder);
        }
        for (Path file : SafeFileWalk.listMatchingFiles(watchFolder, SCAN_DEPTH, this::isSaveCandidate, 500)) {
            handleFile(file);
        }
    }

    private List<Path> listSaveFiles() {
        return SafeFileWalk.listMatchingFiles(watchFolder, SCAN_DEPTH, this::isSaveCandidate, 50);
    }

    private boolean isSaveCandidate(Path file) {
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return SaveFolderDetector.isUploadableFile(
                file, sharedEmulatorFolder ? gameTitle : null,
                sharedEmulatorFolder ? GameTitleTokens.fromTitle(gameTitle) : List.of());
    }

    private void handleFile(Path file) {
        if (!isSaveCandidate(file)) {
            return;
        }
        String key = file.toAbsolutePath().toString();
        String stamp;
        try {
            stamp = Files.getLastModifiedTime(file).toMillis() + "-" + Files.size(file);
        } catch (IOException e) {
            return;
        }
        if (stamp.equals(known.get(key))) {
            return;
        }
        known.put(key, stamp);
        try {
            String relativePath = computeRelativePath(file);
            client.syncFile(gameId, file, relativePath);
            log.accept("Enviado: " + gameTitle + " → " + file.getFileName());
        } catch (MemoryCardClient.UnauthorizedException e) {
            onUnauthorized.run();
        } catch (Exception e) {
            log.accept("Falha em " + file.getFileName() + ": " + e.getMessage());
        }
    }

    private String computeRelativePath(Path file) {
        try {
            Path relative = watchFolder.relativize(file.toAbsolutePath().normalize());
            if (relative.startsWith("..")) {
                return null;
            }
            return relative.toString().replace('\\', '/');
        } catch (Exception e) {
            return null;
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

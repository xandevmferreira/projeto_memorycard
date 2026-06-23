package com.memorycard.sync.watcher;

import com.memorycard.sync.api.ArchiveFileInfo;
import com.memorycard.sync.api.MemoryCardClient;
import com.memorycard.sync.api.RestoreManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public class SaveRestorer {

    private final MemoryCardClient client;
    private final Consumer<String> log;

    public SaveRestorer(MemoryCardClient client, Consumer<String> log) {
        this.client = client;
        this.log = log;
    }

    public int restoreLatest(long gameId, Path saveRoot) throws IOException, InterruptedException {
        RestoreManifest manifest = client.fetchRestoreManifest(gameId);
        if (!manifest.hasFiles() || manifest.cartridgeId() == null) {
            log.accept("Nenhum save no site para restaurar.");
            return 0;
        }

        log.accept("Restaurando fita: " + manifest.cartridgeLabel() + " (" + manifest.files().size() + " arquivo(s))");
        int restored = 0;
        for (ArchiveFileInfo file : manifest.files()) {
            if (!"SAVE".equalsIgnoreCase(file.fileType())) {
                continue;
            }
            Path target = resolveTarget(saveRoot, file);
            if (target == null) {
                log.accept("Ignorado (caminho inválido): " + file.originalFilename());
                continue;
            }
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                Path backup = target.resolveSibling(target.getFileName() + ".memorycard.bak");
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            byte[] data = client.downloadFile(gameId, manifest.cartridgeId(), file.id());
            Files.write(target, data);
            log.accept("Restaurado: " + saveRoot.relativize(target));
            restored++;
        }
        return restored;
    }

    private static Path resolveTarget(Path saveRoot, ArchiveFileInfo file) {
        if (file.relativePath() != null && !file.relativePath().isBlank()) {
            Path resolved = saveRoot.resolve(file.relativePath().replace('/', saveRoot.getFileSystem().getSeparator().charAt(0)));
            if (!resolved.normalize().startsWith(saveRoot.normalize())) {
                return null;
            }
            return resolved;
        }
        return saveRoot.resolve(file.originalFilename());
    }
}

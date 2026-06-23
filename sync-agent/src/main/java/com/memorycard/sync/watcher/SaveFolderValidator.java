package com.memorycard.sync.watcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class SaveFolderValidator {

    public enum Status {
        OK,
        NOT_FOUND,
        INSTALL_FOLDER,
        NO_SAVE_FILES
    }

    public record Check(Status status, int saveFileCount, String message) {}

    private SaveFolderValidator() {}

    /** Validação rápida na abertura — sem varrer arquivos. */
    public static Check validateQuick(Path folder) {
        if (folder == null || folder.toString().isBlank()) {
            return new Check(Status.NOT_FOUND, 0, "");
        }
        if (!Files.isDirectory(folder)) {
            return new Check(Status.NOT_FOUND, 0, "Pasta não existe.");
        }
        if (looksLikeInstallFolder(folder) || SafeFileWalk.isTooBroad(folder)
                || BlockedSavePaths.isBlocked(folder)) {
            return new Check(Status.INSTALL_FOLDER, 0, "Pasta inválida salva.");
        }
        return new Check(Status.OK, 0, "");
    }

    /** Pasta válida para monitoramento contínuo (aceita RetroArch vazio aguardando save). */
    public static boolean canWatch(Path folder, String gameTitle) {
        if (validateQuick(folder).status() != Status.OK) {
            return false;
        }
        if (RetroArchSaveLocator.isRetroArchWatchFolder(folder)) {
            return true;
        }
        if (SaveFolderDetector.countSaveFilesIn(folder) > 0) {
            return true;
        }
        return false;
    }

    public static Check validate(Path folder) {
        if (folder == null || folder.toString().isBlank()) {
            return new Check(Status.NOT_FOUND, 0, "Nenhuma pasta selecionada.");
        }
        if (!Files.isDirectory(folder)) {
            return new Check(Status.NOT_FOUND, 0, "Pasta não existe: " + folder);
        }
        if (looksLikeInstallFolder(folder)) {
            return new Check(Status.INSTALL_FOLDER, 0,
                    "Esta pasta parece ser a instalação do jogo, não os saves.\n"
                            + folder + "\n\n"
                            + "Saves de jogos Xbox ficam em AppData ou Documents\\MicrosoftStore\\RUNE.\n"
                            + "RetroArch: Settings → Directory → Save Files.");
        }
        if (SafeFileWalk.isTooBroad(folder) || BlockedSavePaths.isBlocked(folder)) {
            return new Check(Status.INSTALL_FOLDER, 0,
                    "Pasta muito ampla — escolha a pasta de saves, não a unidade ou XboxGames.");
        }
        if (RetroArchSaveLocator.isRetroArchWatchFolder(folder)) {
            return new Check(Status.OK, 0, "Pasta RetroArch pronta para monitorar.");
        }
        int count = SaveFolderDetector.countSaveFilesIn(folder);
        if (count == 0) {
            return new Check(Status.NO_SAVE_FILES, 0,
                    "Nenhum arquivo de save encontrado nesta pasta.\n"
                            + folder + "\n\n"
                            + "Salve o jogo e tente de novo.");
        }
        return new Check(Status.OK, count,
                count + " arquivo(s) de save prontos para monitorar.");
    }

    static boolean looksLikeInstallFolder(Path folder) {
        String path = folder.toString().toLowerCase(Locale.ROOT).replace('\\', '/');
        if (path.contains("xboxgames") && !path.contains("savegames")
                && !path.contains("/saved/") && !path.contains("/wgs")) {
            return true;
        }
        if (path.contains("windowsapps")) {
            return true;
        }
        try (var stream = Files.list(folder)) {
            boolean hasExe = stream.anyMatch(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe"));
            if (hasExe && !SaveFilePatterns.isSaveDirectoryName(
                    folder.getFileName() != null ? folder.getFileName().toString() : "")) {
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }
}

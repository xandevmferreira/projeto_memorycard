package com.memorycard.sync.watcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Localiza saves do RetroArch via config, instalações conhecidas e processo em execução. */
public final class RetroArchSaveLocator {

    private RetroArchSaveLocator() {}

    public static List<SaveFolderDetector.Candidate> find(String gameTitle, List<String> tokens) {
        List<SaveFolderDetector.Candidate> out = new ArrayList<>();
        for (Path savesDir : discoverSaveRoots()) {
            if (BlockedSavePaths.isBlocked(savesDir) || !Files.isDirectory(savesDir)) {
                continue;
            }
            List<String> matching = matchingSaveNames(savesDir, gameTitle, tokens);
            if (matching.isEmpty()) {
                continue;
            }
            int score = 220 + matching.size() * 10;
            if (Files.exists(savesDir.resolve(matching.getFirst()))) {
                try {
                    if (Files.getLastModifiedTime(savesDir.resolve(matching.getFirst()))
                            .toInstant().isAfter(java.time.Instant.now().minus(java.time.Duration.ofDays(90)))) {
                        score += 20;
                    }
                } catch (IOException ignored) {
                }
            }
            out.add(new SaveFolderDetector.Candidate(
                    savesDir,
                    matching.size(),
                    score,
                    true,
                    "RetroArch — " + matching.getFirst()));
        }
        return out;
    }

    public static boolean hasGameSaveInFolder(Path folder, String gameTitle, List<String> tokens) {
        if (folder == null || !Files.isDirectory(folder) || BlockedSavePaths.isBlocked(folder)) {
            return false;
        }
        if (!isRetroArchWatchFolder(folder)) {
            return false;
        }
        return !matchingSaveNames(folder, gameTitle, tokens).isEmpty();
    }

    public static boolean isRetroArchSavesFolder(Path folder) {
        return isRetroArchNamedFolder(folder, "saves");
    }

    public static boolean isRetroArchStatesFolder(Path folder) {
        return isRetroArchNamedFolder(folder, "states");
    }

    /** Pasta de saves ou save states do RetroArch. */
    public static boolean isRetroArchWatchFolder(Path folder) {
        return isRetroArchSavesFolder(folder) || isRetroArchStatesFolder(folder);
    }

    /** Pasta irmã (saves ↔ states) na mesma instalação portable. */
    public static Optional<Path> companionWatchFolder(Path folder) {
        if (folder == null || folder.getFileName() == null) {
            return Optional.empty();
        }
        Path parent = folder.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        String name = folder.getFileName().toString().toLowerCase(Locale.ROOT);
        Path sibling = switch (name) {
            case "saves" -> parent.resolve("states");
            case "states" -> parent.resolve("saves");
            default -> null;
        };
        if (sibling != null && Files.isDirectory(sibling) && !BlockedSavePaths.isBlocked(sibling)) {
            return Optional.of(sibling.normalize());
        }
        return Optional.empty();
    }

    private static boolean isRetroArchNamedFolder(Path folder, String leafName) {
        if (folder == null || folder.getFileName() == null) {
            return false;
        }
        if (!leafName.equalsIgnoreCase(folder.getFileName().toString())) {
            return false;
        }
        String path = folder.toString().toLowerCase(Locale.ROOT).replace('/', '\\');
        if (path.contains("\\retroarch\\" + leafName) || path.contains("\\retroarch-win64\\" + leafName)) {
            return true;
        }
        Path parent = folder.getParent();
        if (parent != null && parent.getFileName() != null) {
            String parentName = parent.getFileName().toString().toLowerCase(Locale.ROOT);
            if (parentName.contains("retroarch") || parentName.contains("libretro")) {
                return true;
            }
        }
        return path.contains("retroarch") || path.contains("libretro");
    }

    /** Primeira pasta de saves RetroArch encontrada no PC. */
    public static Optional<Path> primarySaveRoot() {
        return discoverSaveRoots().stream().findFirst();
    }

    /** Pasta RetroArch para monitorar. */
    public static Optional<Path> resolveWatchFolder(String gameTitle, List<String> tokens) {
        for (Path root : discoverSaveRoots()) {
            if (!matchingSaveNames(root, gameTitle, tokens).isEmpty()) {
                return Optional.of(root);
            }
        }
        Optional<Path> primary = primarySaveRoot();
        if (primary.isPresent() && ProcessScanner.isRetroArchRunning()) {
            return primary;
        }
        return Optional.empty();
    }

    public static boolean hasGameSaveInAnyRoot(String gameTitle, List<String> tokens) {
        for (Path root : discoverSaveRoots()) {
            if (!matchingSaveNames(root, gameTitle, tokens).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> matchingSaveNames(Path savesDir, String gameTitle, List<String> tokens) {
        return SafeFileWalk.listMatchingFiles(savesDir, 2, file -> {
            if (!SaveFolderDetector.isEmulatorSaveFile(file)) {
                return false;
            }
            return GameTitleTokens.filenameMatches(file.getFileName().toString(), gameTitle)
                    || matchesAnyToken(file.getFileName().toString(), tokens);
        }, 10).stream().map(p -> p.getFileName().toString()).toList();
    }

    private static boolean matchesAnyToken(String filename, List<String> tokens) {
        String norm = SaveFilePatterns.normalize(filename);
        for (String token : tokens) {
            if (token.length() >= 3 && norm.contains(token)) {
                return true;
            }
        }
        return false;
    }

    static Set<Path> discoverSaveRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        addIfDir(roots, envPath("APPDATA", "RetroArch", "saves"));
        addIfDir(roots, envPath("LOCALAPPDATA", "RetroArch", "saves"));
        addIfDir(roots, envPath("USERPROFILE", "Documents", "RetroArch", "saves"));
        addIfDir(roots, envPath("USERPROFILE", "Documents", "RetroArch-Win64", "saves"));

        scanSteamRetroArch(roots);

        Path appDataRetro = envPath("APPDATA", "RetroArch");
        if (Files.isRegularFile(appDataRetro.resolve("retroarch.cfg"))) {
            Path cfg = appDataRetro.resolve("retroarch.cfg");
            addIfDir(roots, readSaveDirFromConfig(cfg, appDataRetro));
            addIfDir(roots, readStateDirFromConfig(cfg, appDataRetro));
        }

        scanPackagesRetroArch(roots);
        scanPortableRetroArchInstalls(roots);
        scanRunningRetroArch(roots);

        Path localAppData = envPath("LOCALAPPDATA");
        if (Files.isDirectory(localAppData)) {
            try (Stream<Path> dirs = Files.list(localAppData)) {
                dirs.filter(Files::isDirectory)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains("retroarch"))
                        .map(p -> p.resolve("saves"))
                        .forEach(p -> addIfDir(roots, p));
            } catch (IOException ignored) {
            }
        }
        return roots;
    }

    private static void scanSteamRetroArch(Set<Path> roots) {
        String[] steamRoots = {
                "C:\\Program Files (x86)\\Steam\\steamapps\\common\\RetroArch\\saves",
                "C:\\Program Files\\Steam\\steamapps\\common\\RetroArch\\saves",
                "D:\\Steam\\steamapps\\common\\RetroArch\\saves",
                "E:\\Steam\\steamapps\\common\\RetroArch\\saves"
        };
        for (String path : steamRoots) {
            addIfDir(roots, Path.of(path));
        }
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null) {
            addIfDir(roots, Path.of(programFilesX86, "Steam", "steamapps", "common", "RetroArch", "saves"));
        }
    }

    private static void scanPackagesRetroArch(Set<Path> roots) {
        Path packages = envPath("LOCALAPPDATA", "Packages");
        if (!Files.isDirectory(packages)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(packages)) {
            dirs.filter(Files::isDirectory)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.contains("retroarch") || n.contains("libretro");
                    })
                    .forEach(pkg -> {
                        addIfDir(roots, pkg.resolve("LocalState").resolve("saves"));
                        Path cfg = pkg.resolve("LocalState").resolve("retroarch.cfg");
                        if (Files.isRegularFile(cfg)) {
                            addIfDir(roots, readSaveDirFromConfig(cfg, pkg.resolve("LocalState")));
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static void scanPortableRetroArchInstalls(Set<Path> roots) {
        String[] drives = {"C", "D", "E", "F", "G", "H"};
        String[] names = {"RetroArch-Win64", "RetroArch", "retroarch"};
        for (String drive : drives) {
            Path driveRoot = Path.of(drive + ":\\");
            if (!Files.isDirectory(driveRoot)) {
                continue;
            }
            for (String name : names) {
                registerRetroArchInstall(roots, driveRoot.resolve(name));
            }
        }
    }

    private static void registerRetroArchInstall(Set<Path> roots, Path base) {
        if (base == null || base.toString().isBlank()) {
            return;
        }
        Path cfg = base.resolve("retroarch.cfg");
        if (Files.isRegularFile(cfg)) {
            addIfDir(roots, readSaveDirFromConfig(cfg, base));
            addIfDir(roots, readStateDirFromConfig(cfg, base));
        }
        addIfDir(roots, base.resolve("saves"));
        addIfDir(roots, base.resolve("states"));
    }

    private static void scanRunningRetroArch(Set<Path> roots) {
        for (ProcessScanner.RunningProcess proc : ProcessScanner.listRunning()) {
            String name = proc.name().toLowerCase(Locale.ROOT);
            if (!name.contains("retroarch")) {
                continue;
            }
            Path exe = Path.of(proc.commandLine());
            Path dir = exe.getParent();
            if (dir == null) {
                continue;
            }
            registerRetroArchInstall(roots, dir);
        }
    }

    private static Path readSaveDirFromConfig(Path cfgFile, Path baseDir) {
        return readDirFromConfig(cfgFile, baseDir, "savefile_directory", "saves");
    }

    private static Path readStateDirFromConfig(Path cfgFile, Path baseDir) {
        return readDirFromConfig(cfgFile, baseDir, "savestate_directory", "states");
    }

    private static Path readDirFromConfig(Path cfgFile, Path baseDir, String key, String defaultLeaf) {
        try {
            for (String line : Files.readAllLines(cfgFile, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (!line.startsWith(key)) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String value = line.substring(eq + 1).trim().replace("\"", "");
                if (value.isBlank() || "default".equalsIgnoreCase(value)) {
                    return baseDir.resolve(defaultLeaf);
                }
                if (value.startsWith(":") || value.startsWith("\\")) {
                    return baseDir.resolve(defaultLeaf);
                }
                Path resolved = Path.of(value);
                if (!resolved.isAbsolute()) {
                    resolved = baseDir.resolve(value);
                }
                return resolved.normalize();
            }
        } catch (IOException ignored) {
        }
        return baseDir.resolve(defaultLeaf);
    }

    private static Path envPath(String... parts) {
        String base = System.getenv(parts[0]);
        if (base == null || base.isBlank()) {
            return Path.of("");
        }
        Path path = Path.of(base);
        for (int i = 1; i < parts.length; i++) {
            path = path.resolve(parts[i]);
        }
        return path;
    }

    private static void addIfDir(Set<Path> roots, Path path) {
        if (path != null && !path.toString().isBlank() && Files.isDirectory(path)
                && !BlockedSavePaths.isBlocked(path)) {
            roots.add(path.normalize());
        }
    }
}

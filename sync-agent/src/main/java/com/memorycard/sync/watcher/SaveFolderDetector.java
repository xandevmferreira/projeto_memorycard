package com.memorycard.sync.watcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Procura pastas de save no PC com base no título do jogo (Windows).
 * Prioriza emuladores conhecidos (RetroArch) e rejeita caches do sistema.
 */
public class SaveFolderDetector {

    private static final int MAX_DEPTH = 5;
    private static final int MAX_CANDIDATES = 40;

    private static final Set<String> SKIP_DIR_NAMES = Set.of(
            "windows", "program files", "program files (x86)", "programdata",
            "node_modules", ".git", "cache", "temp", "tmp", "comms", "unistore",
            "google", "mozilla", "nvidia", "amd", "packages\\microsoft"
    );

    public record Result(Path folder, int saveFileCount, String hint, List<String> sampleFiles) {}

    public record Candidate(Path folder, int saveFileCount, int score, boolean saveDirMatch, String hint) {}

    public Optional<Result> detect(String gameTitle) {
        if (gameTitle == null || gameTitle.isBlank()) {
            return Optional.empty();
        }

        List<String> tokens = GameTitleTokens.fromTitle(gameTitle);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }

        List<Candidate> candidates = new ArrayList<>();

        candidates.addAll(RetroArchSaveLocator.find(gameTitle, tokens));

        for (Path root : searchRoots()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            quickScanKnownPatterns(root, tokens, gameTitle, candidates);
        }

        for (Path root : narrowScanRoots()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            scanRoot(root, tokens, gameTitle, 0, candidates);
        }

        Optional<Result> best = candidates.stream()
                .filter(c -> !BlockedSavePaths.isBlocked(c.folder()))
                .filter(c -> GameTitleMatch.pathMatchesGame(c.folder(), gameTitle, tokens))
                .sorted(Comparator.comparingInt(Candidate::score).reversed())
                .filter(c -> c.saveFileCount() > 0)
                .map(c -> new Result(
                        c.folder(),
                        c.saveFileCount(),
                        c.hint(),
                        sampleFileNames(c.folder(), gameTitle, tokens)))
                .findFirst();
        if (best.isPresent()) {
            return best;
        }

        Optional<Path> retroWatch = RetroArchSaveLocator.resolveWatchFolder(gameTitle, tokens);
        if (retroWatch.isPresent()) {
            Path folder = retroWatch.get();
            return Optional.of(new Result(folder, 0, "RetroArch — aguardando save", List.of()));
        }
        return Optional.empty();
    }

    public static boolean isSharedEmulatorFolder(Path folder) {
        return RetroArchSaveLocator.isRetroArchWatchFolder(folder);
    }

    private static List<String> sampleFileNames(Path folder, String gameTitle, List<String> tokens) {
        return SafeFileWalk.listMatchingFiles(folder, 3, file -> isUploadableFile(file, gameTitle, tokens), 5).stream()
                .map(p -> p.getFileName().toString())
                .toList();
    }

    static boolean isUploadableFile(Path file) {
        return isUploadableFile(file, null, List.of());
    }

    static boolean isUploadableFile(Path file, String gameTitle, List<String> tokens) {
        if (file.getFileName() == null || BlockedSavePaths.isBlocked(file)) {
            return false;
        }
        String name = file.getFileName().toString();
        if (name.startsWith(".") || name.endsWith(".log")) {
            return false;
        }
        if (!isEmulatorSaveFile(file) && !isInsideKnownSaveDirectory(file)) {
            return false;
        }
        if (gameTitle != null && isSharedEmulatorFolder(findSaveRoot(file))) {
            return GameTitleTokens.filenameMatches(name, gameTitle)
                    || matchesAnyToken(name, tokens);
        }
        return true;
    }

    static boolean isEmulatorSaveFile(Path file) {
        String name = file.getFileName().toString();
        String ext = SaveFilePatterns.extension(name);
        if (SaveFilePatterns.EMULATOR_SAVE_EXTENSIONS.contains(ext)) {
            return true;
        }
        return isInsideKnownSaveDirectory(file) && ext.isEmpty();
    }

    private static Path findSaveRoot(Path file) {
        for (Path p = file.getParent(); p != null; p = p.getParent()) {
            if (RetroArchSaveLocator.isRetroArchWatchFolder(p)) {
                return p;
            }
        }
        return file.getParent();
    }

    private static boolean matchesAnyToken(String filename, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return true;
        }
        String norm = SaveFilePatterns.normalize(filename);
        for (String token : tokens) {
            if (token.length() >= 3 && norm.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideKnownSaveDirectory(Path file) {
        for (Path p = file.getParent(); p != null; p = p.getParent()) {
            if (p.getFileName() != null
                    && SaveFilePatterns.isSaveDirectoryName(p.getFileName().toString())) {
                return !BlockedSavePaths.isBlocked(p);
            }
        }
        return false;
    }

    private static int countSaveFiles(Path dir, String gameTitle, List<String> tokens) {
        if (!Files.isDirectory(dir) || SafeFileWalk.isTooBroad(dir) || BlockedSavePaths.isBlocked(dir)) {
            return 0;
        }
        return SafeFileWalk.countMatchingFiles(dir, 3, f -> isUploadableFile(f, gameTitle, tokens));
    }

    private static int countSaveFiles(Path dir) {
        return countSaveFiles(dir, null, List.of());
    }

    public static int countSaveFilesIn(Path folder) {
        return countSaveFiles(folder);
    }

    /** Raízes para padrões conhecidos (Xbox, Packages). */
    private static List<Path> searchRoots() {
        List<Path> roots = new ArrayList<>();
        Path rune = Path.of("C:", "Users", "Public", "Documents", "MicrosoftStore", "RUNE");
        if (Files.isDirectory(rune)) {
            roots.add(rune);
        }
        Path packages = Path.of(System.getenv().getOrDefault("LOCALAPPDATA", ""), "Packages");
        if (Files.isDirectory(packages)) {
            roots.add(packages);
        }
        return roots;
    }

    /** Raízes seguras para varredura genérica — não varre AppData inteiro. */
    private static List<Path> narrowScanRoots() {
        List<Path> roots = new ArrayList<>();
        addEnv(roots, "USERPROFILE", "Documents");
        addEnv(roots, "USERPROFILE", "Saved Games");
        addEnv(roots, "APPDATA", "RetroArch");
        addEnv(roots, "LOCALAPPDATA", "RetroArch");
        return roots;
    }

    private static void addEnv(List<Path> roots, String var, String... sub) {
        String base = System.getenv(var);
        if (base == null || base.isBlank()) {
            return;
        }
        Path path = Path.of(base, sub);
        if (Files.isDirectory(path) && !BlockedSavePaths.isBlocked(path)) {
            roots.add(path);
        }
    }

    private static void quickScanKnownPatterns(Path root, List<String> tokens, String title, List<Candidate> out) {
        String normTitle = SaveFilePatterns.normalize(title);

        if (root.toString().toLowerCase(Locale.ROOT).contains("microsoftstore")) {
            try (Stream<Path> dirs = Files.list(root)) {
                dirs.filter(Files::isDirectory).forEach(gameDir -> {
                    if (matchesTitle(gameDir.getFileName().toString(), tokens, normTitle)) {
                        findSaveDirs(gameDir, 0, 4, title, tokens).forEach(saveDir -> {
                            int count = countSaveFiles(saveDir, title, tokens);
                            if (count > 0) {
                                out.add(new Candidate(saveDir, count, 150, true,
                                        "Microsoft Store / Xbox Game Pass"));
                            }
                        });
                    }
                });
            } catch (IOException ignored) {
            }
        }

        if (root.getFileName() != null && "Packages".equalsIgnoreCase(root.getFileName().toString())) {
            try (Stream<Path> dirs = Files.list(root)) {
                dirs.filter(Files::isDirectory).forEach(pkg -> {
                    String pkgName = pkg.getFileName().toString();
                    if (!matchesTitle(pkgName, tokens, normTitle)) {
                        return;
                    }
                    Path wgs = pkg.resolve("SystemAppData").resolve("wgs");
                    if (Files.isDirectory(wgs)) {
                        int count = countSaveFiles(wgs, title, tokens);
                        if (count > 0) {
                            out.add(new Candidate(wgs, count, 160, true,
                                    "Save Xbox (AppData\\Packages)"));
                        }
                    }
                    findSaveDirs(pkg, 0, 5, title, tokens).forEach(saveDir -> {
                        int count = countSaveFiles(saveDir, title, tokens);
                        if (count > 0) {
                            out.add(new Candidate(saveDir, count, 140, true,
                                    "Save Microsoft Store"));
                        }
                    });
                });
            } catch (IOException ignored) {
            }
        }
    }

    private static void scanRoot(Path dir, List<String> tokens, String title, int depth, List<Candidate> out) {
        if (depth > MAX_DEPTH || out.size() >= MAX_CANDIDATES || shouldSkip(dir)) {
            return;
        }

        String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
        String normTitle = SaveFilePatterns.normalize(title);
        boolean titleMatch = matchesTitle(dirName, tokens, normTitle);

        if (titleMatch && !SaveFolderValidator.looksLikeInstallFolder(dir)) {
            int count = countSaveFiles(dir, title, tokens);
            if (count > 0) {
                int score = count * 5 + 50;
                if (SaveFilePatterns.isSaveDirectoryName(dirName)) {
                    score += 35;
                }
                score += recencyBonus(dir);
                out.add(new Candidate(dir, count, score, SaveFilePatterns.isSaveDirectoryName(dirName),
                        buildHint(dir, SaveFilePatterns.isSaveDirectoryName(dirName))));
            }
        }

        if (depth >= MAX_DEPTH) {
            return;
        }
        try (Stream<Path> children = Files.list(dir)) {
            children.filter(Files::isDirectory)
                    .limit(60)
                    .forEach(child -> scanRoot(child, tokens, title, depth + 1, out));
        } catch (IOException ignored) {
        }
    }

    private static List<Path> findSaveDirs(Path dir, int depth, int maxDepth, String title, List<String> tokens) {
        List<Path> found = new ArrayList<>();
        collectSaveDirs(dir, depth, maxDepth, found, title, tokens);
        return found;
    }

    private static void collectSaveDirs(Path dir, int depth, int maxDepth, List<Path> found,
                                        String title, List<String> tokens) {
        if (depth > maxDepth || shouldSkip(dir)) {
            return;
        }
        if (SaveFilePatterns.isSaveDirectoryName(
                dir.getFileName() != null ? dir.getFileName().toString() : "")
                && countSaveFiles(dir, title, tokens) > 0) {
            found.add(dir);
            return;
        }
        if (depth >= maxDepth) {
            return;
        }
        try (Stream<Path> children = Files.list(dir)) {
            children.filter(Files::isDirectory).forEach(child ->
                    collectSaveDirs(child, depth + 1, maxDepth, found, title, tokens));
        } catch (IOException ignored) {
        }
    }

    private static int recencyBonus(Path dir) {
        try (Stream<Path> files = Files.walk(dir, 2)) {
            Optional<Instant> latest = files
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant();
                        } catch (IOException e) {
                            return Instant.EPOCH;
                        }
                    })
                    .max(Instant::compareTo);
            if (latest.isPresent() && latest.get().isAfter(Instant.now().minus(60, ChronoUnit.DAYS))) {
                return 15;
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return 0;
    }

    private static boolean matchesTitle(String name, List<String> tokens, String normTitle) {
        String norm = SaveFilePatterns.normalize(name);
        if (norm.isEmpty()) {
            return false;
        }
        if (norm.contains(normTitle) || normTitle.contains(norm)) {
            return true;
        }
        int hits = 0;
        for (String token : tokens) {
            if (norm.contains(token)) {
                hits++;
            }
        }
        return hits >= Math.min(2, tokens.size()) || (tokens.size() == 1 && hits == 1);
    }

    private static boolean shouldSkip(Path dir) {
        if (BlockedSavePaths.isBlocked(dir)) {
            return true;
        }
        String path = dir.toString().toLowerCase(Locale.ROOT);
        for (String skip : SKIP_DIR_NAMES) {
            if (path.contains(skip)) {
                return true;
            }
        }
        return false;
    }

    private static String buildHint(Path dir, boolean saveDir) {
        String p = dir.toString();
        if (p.contains("RetroArch")) {
            return "RetroArch";
        }
        if (p.contains("MicrosoftStore")) {
            return "Microsoft Store / Xbox Game Pass";
        }
        if (p.contains("Steam")) {
            return "Steam";
        }
        if (saveDir) {
            return "Pasta de saves";
        }
        return "Arquivos de save encontrados";
    }
}

package com.memorycard.sync.watcher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class SafeFileWalk {

    private SafeFileWalk() {}

    public static boolean isTooBroad(Path folder) {
        if (folder == null) {
            return true;
        }
        Path abs = folder.toAbsolutePath().normalize();
        if (abs.getNameCount() == 0) {
            return true;
        }
        Path root = abs.getRoot();
        if (root != null && abs.equals(root)) {
            return true;
        }
        // ex.: C:\XboxGames ou C:\Users — pouco específico para monitorar
        if (abs.getNameCount() <= 1 && root != null && abs.startsWith(root)) {
            String name = abs.getNameCount() == 1 ? abs.getName(0).toString().toLowerCase(Locale.ROOT) : "";
            if (!SaveFilePatterns.isSaveDirectoryName(name)) {
                return true;
            }
        }
        return false;
    }

    public static int countMatchingFiles(Path dir, int maxDepth, java.util.function.Predicate<Path> matcher) {
        if (!Files.isDirectory(dir) || isTooBroad(dir) || BlockedSavePaths.isBlocked(dir)) {
            return 0;
        }
        int[] count = {0};
        try {
            Files.walkFileTree(dir, EnumSet.noneOf(java.nio.file.FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (count[0] >= 20) {
                        return FileVisitResult.TERMINATE;
                    }
                    try {
                        if (attrs.isRegularFile() && attrs.size() <= 64L * 1024 * 1024 && matcher.test(file)) {
                            count[0]++;
                        }
                    } catch (Exception ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (shouldSkipDir(dir) || BlockedSavePaths.isBlocked(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
        return count[0];
    }

    public static List<Path> listMatchingFiles(Path dir, int maxDepth, Predicate<Path> matcher, int limit) {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(dir) || isTooBroad(dir) || BlockedSavePaths.isBlocked(dir)) {
            return files;
        }
        try {
            Files.walkFileTree(dir, EnumSet.noneOf(java.nio.file.FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (files.size() >= limit) {
                        return FileVisitResult.TERMINATE;
                    }
                    try {
                        if (attrs.isRegularFile() && matcher.test(file)) {
                            files.add(file);
                        }
                    } catch (Exception ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                    if (shouldSkipDir(d) || BlockedSavePaths.isBlocked(d)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
        return files;
    }

    private static boolean shouldSkipDir(Path dir) {
        if (BlockedSavePaths.isBlocked(dir)) {
            return true;
        }
        String name = dir.getFileName() != null ? dir.getFileName().toString().toLowerCase(Locale.ROOT) : "";
        return name.equals("$recycle.bin") || name.equals("system volume information")
                || name.equals("windows") || name.equals("program files")
                || name.equals("program files (x86)") || name.equals("comms")
                || name.equals("unistore") || name.equals("unistoredb");
    }
}

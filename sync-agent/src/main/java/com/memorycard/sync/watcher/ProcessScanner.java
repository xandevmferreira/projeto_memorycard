package com.memorycard.sync.watcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProcessScanner {

    private ProcessScanner() {}

    public record RunningProcess(long pid, String name, String commandLine) {}

    public static boolean isRetroArchRunning() {
        return listRunning().stream()
                .anyMatch(p -> p.name().toLowerCase(Locale.ROOT).contains("retroarch"));
    }

    public static List<RunningProcess> listRunning() {
        List<RunningProcess> processes = new ArrayList<>();
        ProcessHandle.allProcesses()
                .filter(ProcessHandle::isAlive)
                .forEach(handle -> {
                    ProcessHandle.Info info = handle.info();
                    String command = info.command().orElse("");
                    if (command.isBlank()) {
                        return;
                    }
                    String name = Path.of(command).getFileName().toString();
                    processes.add(new RunningProcess(handle.pid(), name, command));
                });
        return processes;
    }

    public static String normalizeProcessName(String name) {
        if (name == null) {
            return "";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".exe")) {
            lower = lower.substring(0, lower.length() - 4);
        }
        return SaveFilePatterns.normalize(lower).replace(" ", "");
    }
}

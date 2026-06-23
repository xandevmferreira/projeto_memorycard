package com.memorycard.sync.watcher;

import java.nio.file.Path;
import java.util.Locale;

/** Caminhos que nunca são pasta de save de jogo (cache do Windows, Mail, etc.). */
public final class BlockedSavePaths {

    private static final String[] BLOCKED_SEGMENTS = {
            "\\comms\\unistore", "\\unistoredb\\", "\\microsoft\\windows\\",
            "\\google\\chrome", "\\mozilla\\firefox", "\\discord\\",
            "\\spotify\\", "\\cache\\", "\\caches\\", "\\temp\\",
            "\\tmp\\", "\\inetcache\\", "\\wercache\\", "\\d3dscache\\",
            "\\nvidia\\", "\\amd\\", "\\microsoft\\edge\\",
            "\\windowsapps\\", "\\application data\\microsoft\\"
    };

    private BlockedSavePaths() {}

    public static boolean isBlocked(Path path) {
        if (path == null) {
            return true;
        }
        String normalized = path.toString().toLowerCase(Locale.ROOT).replace('/', '\\');
        if (!normalized.endsWith("\\")) {
            normalized += "\\";
        }
        for (String segment : BLOCKED_SEGMENTS) {
            if (normalized.contains(segment)) {
                return true;
            }
        }
        return false;
    }
}

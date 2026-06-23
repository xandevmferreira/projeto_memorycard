package com.memorycard.service;

import java.util.List;
import java.util.Optional;

/** Plataformas sugeridas no cadastro de jogos (fonte única com o player web). */
public final class SupportedPlatforms {

    public record Option(String value, String label, boolean webPlayer, String group) {}

    private static final List<Option> ALL = List.of(
            // Nintendo — console no navegador
            new Option("SNES", "Super Nintendo (SNES)", true, "web"),
            new Option("NES", "Nintendo (NES)", true, "web"),
            new Option("Nintendo 64", "Nintendo 64", true, "web"),
            new Option("Nintendo DS", "Nintendo DS", true, "web"),
            new Option("Game Boy Advance", "Game Boy Advance", true, "web"),
            new Option("Game Boy Color", "Game Boy Color", true, "web"),
            new Option("Game Boy", "Game Boy", true, "web"),
            new Option("Virtual Boy", "Virtual Boy", true, "web"),

            // Sega
            new Option("Mega Drive", "Sega Mega Drive / Genesis", true, "web"),
            new Option("Master System", "Sega Master System", true, "web"),
            new Option("Game Gear", "Sega Game Gear", true, "web"),
            new Option("Sega CD", "Sega CD", true, "web"),
            new Option("Sega 32X", "Sega 32X", true, "web"),
            new Option("Sega Saturn", "Sega Saturn", true, "web"),

            // Sony
            new Option("PlayStation", "PlayStation (PS1)", true, "web"),
            new Option("PSP", "PlayStation Portable", true, "web"),

            // Outros retro (web)
            new Option("PC Engine", "PC Engine / TurboGrafx-16", true, "web"),
            new Option("Neo Geo Pocket", "Neo Geo Pocket", true, "web"),
            new Option("WonderSwan", "WonderSwan", true, "web"),
            new Option("Atari 2600", "Atari 2600", true, "web"),
            new Option("Atari 7800", "Atari 7800", true, "web"),
            new Option("Atari Lynx", "Atari Lynx", true, "web"),
            new Option("Commodore 64", "Commodore 64", true, "web"),
            new Option("ColecoVision", "ColecoVision", true, "web"),
            new Option("3DO", "3DO", true, "web"),
            new Option("Amiga", "Amiga", true, "web"),
            new Option("Arcade", "Arcade / MAME", true, "web"),

            // Moderno — fitas via upload / zip, sem player web
            new Option("PC", "PC (Windows/Linux/Mac)", false, "modern"),
            new Option("PlayStation 5", "PlayStation 5", false, "modern"),
            new Option("PlayStation 4", "PlayStation 4", false, "modern"),
            new Option("Xbox Series", "Xbox Series X|S", false, "modern"),
            new Option("Xbox One", "Xbox One", false, "modern"),
            new Option("Nintendo Switch", "Nintendo Switch", false, "modern"),
            new Option("Nintendo Switch 2", "Nintendo Switch 2", false, "modern"),
            new Option("Steam Deck", "Steam Deck", false, "modern"),
            new Option("Android", "Android / Mobile", false, "modern"),
            new Option("iOS", "iOS / iPhone / iPad", false, "modern")
    );

    private SupportedPlatforms() {}

    public static List<Option> forForm() {
        return ALL;
    }

    public static boolean supportsWebPlayer(String platform) {
        return WebPlayerCoreMapper.coreForPlatform(platform).isPresent();
    }

    public static Optional<Option> match(String platform) {
        if (platform == null || platform.isBlank()) {
            return Optional.empty();
        }
        String norm = platform.trim().toLowerCase();
        return ALL.stream()
                .filter(o -> o.value().equalsIgnoreCase(platform.trim())
                        || o.label().toLowerCase().contains(norm)
                        || norm.contains(o.value().toLowerCase()))
                .findFirst();
    }
}

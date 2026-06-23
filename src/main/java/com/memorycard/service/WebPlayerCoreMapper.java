package com.memorycard.service;

import java.util.Locale;
import java.util.Optional;

/** Mapeia plataforma do jogo para core do EmulatorJS. */
public final class WebPlayerCoreMapper {

    private WebPlayerCoreMapper() {}

    public static Optional<String> coreForPlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return Optional.empty();
        }
        String p = platform.toLowerCase(Locale.ROOT);

        // Nintendo — 8/16 bits
        if (containsAny(p, "snes", "super nintendo", "super famicom", "sfam")) {
            return Optional.of("snes9x");
        }
        if (containsAny(p, "nes", "famicom", "nintendo entertainment")) {
            return Optional.of("nestopia");
        }
        if (containsAny(p, "virtual boy", "vb")) {
            return Optional.of("beetle_vb");
        }
        if (containsAny(p, "nintendo ds", "nds")) {
            return Optional.of("melonds");
        }
        if (containsAny(p, "nintendo 64", "n64")) {
            return Optional.of("mupen64plus_next");
        }

        // Nintendo — portáteis
        if (containsAny(p, "game boy advance", "gba")) {
            return Optional.of("mgba");
        }
        if (containsAny(p, "game boy color", "gbc")) {
            return Optional.of("gambatte");
        }
        if (containsAny(p, "game boy", "gb ")) {
            return Optional.of("gambatte");
        }

        // Sega
        if (containsAny(p, "sega cd", "mega cd", "segacd")) {
            return Optional.of("genesis_plus_gx");
        }
        if (containsAny(p, "sega 32x", "32x")) {
            return Optional.of("picodrive");
        }
        if (containsAny(p, "game gear", "sega gg")) {
            return Optional.of("genesis_plus_gx");
        }
        if (containsAny(p, "master system", "sega master", "sms")) {
            return Optional.of("smsplus");
        }
        if (containsAny(p, "mega drive", "genesis", "sega md", "sega genesis")) {
            return Optional.of("genesis_plus_gx");
        }
        if (containsAny(p, "sega saturn", "saturn")) {
            return Optional.of("yabause");
        }

        // Sony
        if (containsAny(p, "playstation", "ps1", "psx", "ps one")) {
            return Optional.of("pcsx_rearmed");
        }
        if (containsAny(p, "psp", "playstation portable")) {
            return Optional.of("ppsspp");
        }

        // NEC / SNK / Bandai
        if (containsAny(p, "pc engine", "pc-engine", "turbografx", "turbo grafx", "tg16")) {
            return Optional.of("mednafen_pce");
        }
        if (containsAny(p, "pc-fx", "pcfx")) {
            return Optional.of("mednafen_pcfx");
        }
        if (containsAny(p, "neo geo pocket color", "ngpc")) {
            return Optional.of("mednafen_ngp");
        }
        if (containsAny(p, "neo geo pocket", "ngp")) {
            return Optional.of("mednafen_ngp");
        }
        if (containsAny(p, "wonderswan color", "wsc")) {
            return Optional.of("mednafen_wswan");
        }
        if (containsAny(p, "wonderswan", "ws")) {
            return Optional.of("mednafen_wswan");
        }

        // Atari
        if (containsAny(p, "atari 2600", "2600")) {
            return Optional.of("stella2014");
        }
        if (containsAny(p, "atari 5200", "5200")) {
            return Optional.of("a5200");
        }
        if (containsAny(p, "atari 7800", "7800")) {
            return Optional.of("prosystem");
        }
        if (containsAny(p, "atari lynx", "lynx")) {
            return Optional.of("handy");
        }
        if (containsAny(p, "atari jaguar", "jaguar")) {
            return Optional.of("virtualjaguar");
        }

        // Outros clássicos
        if (containsAny(p, "commodore 64", "c64")) {
            return Optional.of("vice_x64sc");
        }
        if (containsAny(p, "coleco", "colecovision")) {
            return Optional.of("gearcoleco");
        }
        if (containsAny(p, "3do")) {
            return Optional.of("opera");
        }
        if (containsAny(p, "amiga")) {
            return Optional.of("puae");
        }
        if (containsAny(p, "arcade", "mame", "neo geo", "cps1", "cps2", "fbneo")) {
            return Optional.of("fbneo");
        }

        return Optional.empty();
    }

    public static String romExtensionsHint(String core) {
        return switch (core) {
            case "snes9x" -> ".sfc, .smc";
            case "nestopia" -> ".nes";
            case "beetle_vb" -> ".vb, .vboy";
            case "melonds" -> ".nds";
            case "mupen64plus_next" -> ".n64, .z64, .v64";
            case "mgba" -> ".gba";
            case "gambatte" -> ".gb, .gbc";
            case "genesis_plus_gx" -> ".md, .gen, .smd, .gg, .bin, .cue, .iso";
            case "picodrive" -> ".32x, .bin";
            case "smsplus" -> ".sms";
            case "yabause" -> ".iso, .cue, .bin";
            case "pcsx_rearmed" -> ".bin, .cue, .chd, .pbp";
            case "ppsspp" -> ".iso, .cso, .pbp";
            case "mednafen_pce" -> ".pce";
            case "mednafen_pcfx" -> ".cue, .iso";
            case "mednafen_ngp" -> ".ngp, .ngc";
            case "mednafen_wswan" -> ".ws, .wsc";
            case "stella2014" -> ".a26";
            case "a5200" -> ".a52";
            case "prosystem" -> ".a78";
            case "handy" -> ".lnx";
            case "virtualjaguar" -> ".j64, .jag";
            case "vice_x64sc" -> ".d64, .t64, .prg";
            case "gearcoleco" -> ".col";
            case "opera" -> ".iso";
            case "puae" -> ".adf, .hdf, .lha";
            case "fbneo" -> ".zip (ROM de arcade)";
            default -> ".rom";
        };
    }

    /** Rótulo amigável na UI. */
    public static String platformLabel(String core) {
        return switch (core) {
            case "snes9x" -> "Super Nintendo";
            case "nestopia" -> "NES";
            case "beetle_vb" -> "Virtual Boy";
            case "melonds" -> "Nintendo DS";
            case "mupen64plus_next" -> "Nintendo 64";
            case "mgba" -> "Game Boy Advance";
            case "gambatte" -> "Game Boy / Color";
            case "genesis_plus_gx" -> "Sega (MD / GG / CD)";
            case "picodrive" -> "Sega 32X";
            case "smsplus" -> "Master System";
            case "yabause" -> "Sega Saturn";
            case "pcsx_rearmed" -> "PlayStation";
            case "ppsspp" -> "PSP";
            case "mednafen_pce" -> "PC Engine";
            case "mednafen_pcfx" -> "PC-FX";
            case "mednafen_ngp" -> "Neo Geo Pocket";
            case "mednafen_wswan" -> "WonderSwan";
            case "stella2014" -> "Atari 2600";
            case "a5200" -> "Atari 5200";
            case "prosystem" -> "Atari 7800";
            case "handy" -> "Atari Lynx";
            case "virtualjaguar" -> "Atari Jaguar";
            case "vice_x64sc" -> "Commodore 64";
            case "gearcoleco" -> "ColecoVision";
            case "opera" -> "3DO";
            case "puae" -> "Amiga";
            case "fbneo" -> "Arcade";
            default -> "Retro";
        };
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

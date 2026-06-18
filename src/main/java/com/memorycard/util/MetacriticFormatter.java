package com.memorycard.util;

import java.math.BigDecimal;

public final class MetacriticFormatter {

    private MetacriticFormatter() {}

    public static String format(BigDecimal score, String source) {
        if (score == null) {
            return null;
        }
        String label = switch (source != null ? source : "") {
            case "METACRITIC" -> "Metacritic";
            case "CATALOG" -> "Metacritic (catálogo)";
            case "STEAM" -> "Steam";
            case "RAWG" -> "RAWG";
            default -> "Referência";
        };
        return score.stripTrailingZeros().toPlainString() + "/100 · " + label;
    }
}

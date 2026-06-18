package com.memorycard.entity;

public enum CompletionType {
    STORY,
    HUNDRED_PERCENT,
    SPEEDRUN,
    CASUAL;

    public String getLabel() {
        return switch (this) {
            case STORY -> "História zerada";
            case HUNDRED_PERCENT -> "100% / completion";
            case SPEEDRUN -> "Speedrun / PB";
            case CASUAL -> "Casual / drop-in";
        };
    }
}

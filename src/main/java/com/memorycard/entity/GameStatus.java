package com.memorycard.entity;

public enum GameStatus {
    PLAYING,
    COMPLETED,
    ABANDONED,
    PAUSED;

    public String getLabel() {
        return switch (this) {
            case PLAYING -> "Jogando";
            case COMPLETED -> "Zerado";
            case ABANDONED -> "Abandonado";
            case PAUSED -> "Pausado";
        };
    }
}

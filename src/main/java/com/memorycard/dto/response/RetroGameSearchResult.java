package com.memorycard.dto.response;

public record RetroGameSearchResult(
        String title,
        int raGameId,
        int consoleId,
        String consoleName
) {}

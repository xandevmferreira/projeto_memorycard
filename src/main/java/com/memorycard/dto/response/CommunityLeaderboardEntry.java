package com.memorycard.dto.response;

public record CommunityLeaderboardEntry(
        String playerName,
        long completedGames
) {}

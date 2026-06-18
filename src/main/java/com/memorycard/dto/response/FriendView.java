package com.memorycard.dto.response;

import java.time.Instant;

public record FriendView(
        Long friendshipId,
        Long userId,
        String name,
        Instant friendsSince,
        boolean incoming
) {}

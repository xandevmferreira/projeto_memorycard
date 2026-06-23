package com.memorycard.dto.response;

import com.memorycard.entity.SubscriptionStatus;
import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String nick,
        String displayNick,
        String email,
        SubscriptionStatus subscriptionStatus,
        Instant createdAt
) {}

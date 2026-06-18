package com.memorycard.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {}

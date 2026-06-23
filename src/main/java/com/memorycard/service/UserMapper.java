package com.memorycard.service;

import com.memorycard.dto.response.UserResponse;
import com.memorycard.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getNick(),
                user.getDisplayNick(),
                user.getEmail(),
                user.getSubscriptionStatus(),
                user.getCreatedAt()
        );
    }
}

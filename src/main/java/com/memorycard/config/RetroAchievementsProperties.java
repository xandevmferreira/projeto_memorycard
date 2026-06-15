package com.memorycard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retroachievements")
public record RetroAchievementsProperties(
        boolean enabled,
        String apiKey,
        String baseUrl
) {
    public RetroAchievementsProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://retroachievements.org";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }
}

package com.memorycard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class, RawgProperties.class, GamingNewsProperties.class, RetroAchievementsProperties.class, AppSecurityProperties.class})
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}

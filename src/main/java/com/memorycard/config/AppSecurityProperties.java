package com.memorycard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        boolean cookieSecure,
        boolean requireStrongSecrets
) {}

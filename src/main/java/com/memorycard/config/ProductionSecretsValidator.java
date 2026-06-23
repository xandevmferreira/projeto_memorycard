package com.memorycard.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecretsValidator {

    private static final String DEFAULT_JWT = "change-this-secret-key-to-a-long-random-string-at-least-256-bits";

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final AppSecurityProperties securityProperties;

    public ProductionSecretsValidator(Environment environment,
                                      JwtProperties jwtProperties,
                                      AppSecurityProperties securityProperties) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.securityProperties = securityProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!securityProperties.requireStrongSecrets()) {
            return;
        }
        String jwt = jwtProperties.secret();
        if (jwt == null || jwt.isBlank() || DEFAULT_JWT.equals(jwt) || jwt.length() < 32) {
            throw new IllegalStateException(
                    "Produção: defina JWT_SECRET forte (32+ caracteres) no arquivo .env");
        }
        String dbPassword = environment.getProperty("spring.datasource.password");
        if (dbPassword == null || dbPassword.isBlank() || "memorycard".equals(dbPassword)) {
            throw new IllegalStateException(
                    "Produção: defina POSTGRES_PASSWORD forte no arquivo .env");
        }
    }
}

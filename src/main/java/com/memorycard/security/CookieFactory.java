package com.memorycard.security;

import com.memorycard.config.AppSecurityProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieFactory {

    private final AppSecurityProperties securityProperties;

    public CookieFactory(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public ResponseCookie jwtCookie(String token, Duration maxAge) {
        return ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite(securityProperties.cookieSecure() ? "Strict" : "Lax")
                .build();
    }

    public ResponseCookie clearJwtCookie() {
        return jwtCookie("", Duration.ZERO);
    }
}

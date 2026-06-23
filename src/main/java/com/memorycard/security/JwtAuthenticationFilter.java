package com.memorycard.security;

import com.memorycard.entity.User;
import com.memorycard.repository.UserRepository;
import com.memorycard.service.SyncTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import com.memorycard.security.CookieFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_COOKIE_NAME = "MEMORYCARD_TOKEN";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final SyncTokenService syncTokenService;
    private final CookieFactory cookieFactory;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository,
                                   SyncTokenService syncTokenService,
                                   CookieFactory cookieFactory) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.syncTokenService = syncTokenService;
        this.cookieFactory = cookieFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            if (token.startsWith(SyncTokenService.TOKEN_PREFIX)) {
                syncTokenService.findUserByToken(token).ifPresent(user ->
                        setAuthentication(request, user, true));
            } else if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                userRepository.findById(userId).ifPresent(user -> {
                    setAuthentication(request, user, false);
                    refreshJwtCookie(response, token);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private void refreshJwtCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieFactory.jwtCookie(token, Duration.ofDays(7)).toString());
    }

    private void setAuthentication(HttpServletRequest request, User user, boolean syncToken) {
        var role = syncToken ? "ROLE_SYNC" : "ROLE_USER";
        var authorities = List.of(new SimpleGrantedAuthority(role));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

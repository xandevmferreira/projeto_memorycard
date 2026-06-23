package com.memorycard.service;

import com.memorycard.dto.request.LoginRequest;
import com.memorycard.dto.request.RegisterRequest;
import com.memorycard.dto.response.AuthResponse;
import com.memorycard.entity.SubscriptionStatus;
import com.memorycard.entity.User;
import com.memorycard.exception.EmailAlreadyExistsException;
import com.memorycard.exception.InvalidCredentialsException;
import com.memorycard.repository.UserRepository;
import com.memorycard.security.CookieFactory;
import com.memorycard.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieFactory cookieFactory;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, CookieFactory cookieFactory) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieFactory = cookieFactory;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setSubscriptionStatus(SubscriptionStatus.FREE);

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, UserMapper.toResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserMapper.toResponse(user));
    }

    public void setJwtCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieFactory.jwtCookie(token, Duration.ofDays(7)).toString());
    }

    public void clearJwtCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearJwtCookie().toString());
    }
}

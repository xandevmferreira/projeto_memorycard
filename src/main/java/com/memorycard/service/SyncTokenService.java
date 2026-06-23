package com.memorycard.service;

import com.memorycard.entity.User;
import com.memorycard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class SyncTokenService {

    public static final String TOKEN_PREFIX = "mc_sync_";

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public SyncTokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasActiveToken(User user) {
        return user.getSyncTokenHash() != null && !user.getSyncTokenHash().isBlank();
    }

    @Transactional
    public String generateToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = TOKEN_PREFIX + HexFormat.of().formatHex(bytes);
        user.setSyncTokenHash(hash(token));
        user.setSyncTokenCreatedAt(Instant.now());
        userRepository.save(user);
        return token;
    }

    @Transactional
    public void revokeToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        user.setSyncTokenHash(null);
        user.setSyncTokenCreatedAt(null);
        userRepository.save(user);
    }

    public Optional<User> findUserByToken(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX) || token.length() < TOKEN_PREFIX.length() + 16) {
            return Optional.empty();
        }
        String hash = hash(token);
        return userRepository.findBySyncTokenHash(hash);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}

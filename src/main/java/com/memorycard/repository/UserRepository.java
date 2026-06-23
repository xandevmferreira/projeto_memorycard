package com.memorycard.repository;

import com.memorycard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickIgnoreCase(String nick);

    boolean existsByEmail(String email);

    boolean existsByNickIgnoreCase(String nick);

    java.util.Optional<User> findBySyncTokenHash(String syncTokenHash);
}

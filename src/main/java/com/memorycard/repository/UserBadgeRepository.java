package com.memorycard.repository;

import com.memorycard.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    Optional<UserBadge> findByUserIdAndBadgeId(Long userId, Long badgeId);

    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);
}

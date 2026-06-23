package com.memorycard.repository;

import com.memorycard.entity.PlaySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaySessionRepository extends JpaRepository<PlaySession, Long> {

    Optional<PlaySession> findByUserIdAndGameIdAndEndedAtIsNull(Long userId, Long gameId);

    List<PlaySession> findByUserIdAndEndedAtIsNull(Long userId);
}

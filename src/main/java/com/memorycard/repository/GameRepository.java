package com.memorycard.repository;

import com.memorycard.entity.Game;
import com.memorycard.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Game> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, GameStatus status);

    @Query("SELECT COALESCE(SUM(g.hoursPlayed), 0) FROM Game g WHERE g.userId = :userId")
    BigDecimal sumHoursPlayedByUserId(@Param("userId") Long userId);

    List<Game> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}

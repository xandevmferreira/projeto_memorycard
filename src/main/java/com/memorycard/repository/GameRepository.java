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

    long countByUserIdAndRetroTrue(Long userId);

    @Query("SELECT COUNT(g) FROM Game g WHERE g.userId = :userId AND g.status = com.memorycard.entity.GameStatus.COMPLETED AND g.completedAt >= :from")
    long countCompletedSince(@Param("userId") Long userId, @Param("from") java.time.LocalDate from);

    @Query("SELECT g.platform, COUNT(g) FROM Game g WHERE g.userId = :userId AND g.platform IS NOT NULL GROUP BY g.platform ORDER BY COUNT(g) DESC")
    List<Object[]> countByPlatform(@Param("userId") Long userId);

    @Query("SELECT g FROM Game g WHERE g.userId IN (SELECT u.id FROM User u WHERE u.communityVisible = true) AND g.status = com.memorycard.entity.GameStatus.COMPLETED ORDER BY g.completedAt DESC NULLS LAST, g.createdAt DESC")
    List<Game> findRecentPublicCompletions(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(u.nick, 'Jogador'), COUNT(g) FROM Game g, User u WHERE g.userId = u.id AND u.communityVisible = true AND g.status = com.memorycard.entity.GameStatus.COMPLETED GROUP BY u.id, u.nick ORDER BY COUNT(g) DESC")
    List<Object[]> findCommunityLeaderboard(org.springframework.data.domain.Pageable pageable);
}

package com.memorycard.repository;

import com.memorycard.entity.GameScreenshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameScreenshotRepository extends JpaRepository<GameScreenshot, Long> {

    List<GameScreenshot> findByGameIdOrderByUploadedAtDesc(Long gameId);

    Optional<GameScreenshot> findByIdAndGameId(Long id, Long gameId);
}

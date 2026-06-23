package com.memorycard.repository;

import com.memorycard.entity.GameCartridge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameCartridgeRepository extends JpaRepository<GameCartridge, Long> {

    List<GameCartridge> findByGameIdOrderByCreatedAtDesc(Long gameId);

    List<GameCartridge> findByGameIdInOrderByCreatedAtDesc(List<Long> gameIds);
}

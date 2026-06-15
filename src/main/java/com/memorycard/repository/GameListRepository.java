package com.memorycard.repository;

import com.memorycard.entity.GameList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameListRepository extends JpaRepository<GameList, Long> {

    List<GameList> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GameList> findByIdAndUserId(Long id, Long userId);
}

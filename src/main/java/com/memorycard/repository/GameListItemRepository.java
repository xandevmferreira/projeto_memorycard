package com.memorycard.repository;

import com.memorycard.entity.GameListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameListItemRepository extends JpaRepository<GameListItem, Long> {

    List<GameListItem> findByListIdOrderByPositionAsc(Long listId);

    Optional<GameListItem> findByListIdAndGameId(Long listId, Long gameId);

    boolean existsByListIdAndGameId(Long listId, Long gameId);

    int countByListId(Long listId);
}

package com.memorycard.repository;

import com.memorycard.entity.GameJournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameJournalEntryRepository extends JpaRepository<GameJournalEntry, Long> {

    List<GameJournalEntry> findByGameIdOrderByCreatedAtDesc(Long gameId);

    long countByUserId(Long userId);

    Optional<GameJournalEntry> findByIdAndGameIdAndUserId(Long id, Long gameId, Long userId);
}

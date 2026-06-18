package com.memorycard.service;

import com.memorycard.dto.response.JournalEntryResponse;
import com.memorycard.entity.GameJournalEntry;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameJournalEntryRepository;
import com.memorycard.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameJournalService {

    private final GameJournalEntryRepository journalRepository;
    private final GameRepository gameRepository;

    public GameJournalService(GameJournalEntryRepository journalRepository, GameRepository gameRepository) {
        this.journalRepository = journalRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> findByGame(Long userId, Long gameId) {
        verifyGame(userId, gameId);
        return journalRepository.findByGameIdOrderByCreatedAtDesc(gameId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByUser(Long userId) {
        return journalRepository.countByUserId(userId);
    }

    @Transactional
    public JournalEntryResponse addEntry(Long userId, Long gameId, String content, boolean spoiler) {
        verifyGame(userId, gameId);
        GameJournalEntry entry = new GameJournalEntry();
        entry.setGameId(gameId);
        entry.setUserId(userId);
        entry.setContent(content.trim());
        entry.setSpoiler(spoiler);
        return toResponse(journalRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(Long userId, Long gameId, Long entryId) {
        verifyGame(userId, gameId);
        GameJournalEntry entry = journalRepository.findByIdAndGameIdAndUserId(entryId, gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada não encontrada"));
        journalRepository.delete(entry);
    }

    private void verifyGame(Long userId, Long gameId) {
        gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));
    }

    private JournalEntryResponse toResponse(GameJournalEntry entry) {
        return new JournalEntryResponse(entry.getId(), entry.getContent(), entry.isSpoiler(), entry.getCreatedAt());
    }
}

package com.memorycard.service;

import com.memorycard.dto.response.PlaySessionView;
import com.memorycard.entity.Game;
import com.memorycard.entity.PlaySession;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameRepository;
import com.memorycard.repository.PlaySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PlaySessionService {

    private final PlaySessionRepository playSessionRepository;
    private final GameRepository gameRepository;

    public PlaySessionService(PlaySessionRepository playSessionRepository, GameRepository gameRepository) {
        this.playSessionRepository = playSessionRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public PlaySessionView findActiveForGame(Long userId, Long gameId) {
        return playSessionRepository.findByUserIdAndGameIdAndEndedAtIsNull(userId, gameId)
                .map(session -> toView(session, gameRepository.findById(gameId).orElse(null)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PlaySessionView findAnyActive(Long userId) {
        List<PlaySession> active = playSessionRepository.findByUserIdAndEndedAtIsNull(userId);
        if (active.isEmpty()) {
            return null;
        }
        PlaySession session = active.getFirst();
        return toView(session, gameRepository.findById(session.getGameId()).orElse(null));
    }

    @Transactional
    public PlaySessionView startSession(Long userId, Long gameId, String source, String processName) {
        verifyOwnedGame(userId, gameId);

        playSessionRepository.findByUserIdAndEndedAtIsNull(userId).stream()
                .filter(s -> !s.getGameId().equals(gameId))
                .forEach(s -> finalizeSession(s));

        return playSessionRepository.findByUserIdAndGameIdAndEndedAtIsNull(userId, gameId)
                .map(s -> toView(s, gameRepository.findById(gameId).orElse(null)))
                .orElseGet(() -> {
                    PlaySession session = new PlaySession();
                    session.setUserId(userId);
                    session.setGameId(gameId);
                    session.setSource(source != null && !source.isBlank() ? source.trim() : "WEB");
                    session.setProcessName(blankToNull(processName));
                    PlaySession saved = playSessionRepository.save(session);
                    return toView(saved, gameRepository.findById(gameId).orElse(null));
                });
    }

    @Transactional
    public PlaySessionView stopSession(Long userId, Long gameId) {
        PlaySession session = playSessionRepository.findByUserIdAndGameIdAndEndedAtIsNull(userId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma sessão ativa para este jogo"));
        finalizeSession(session);
        return toView(session, gameRepository.findById(gameId).orElse(null));
    }

    @Transactional
    public PlaySessionView stopAnyActive(Long userId) {
        List<PlaySession> active = playSessionRepository.findByUserIdAndEndedAtIsNull(userId);
        if (active.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma sessão ativa");
        }
        PlaySession session = active.getFirst();
        finalizeSession(session);
        return toView(session, gameRepository.findById(session.getGameId()).orElse(null));
    }

    private void finalizeSession(PlaySession session) {
        Instant endedAt = Instant.now();
        session.setEndedAt(endedAt);
        int minutes = (int) Math.max(1, Duration.between(session.getStartedAt(), endedAt).toMinutes());
        session.setDurationMinutes(minutes);
        playSessionRepository.save(session);
        addMinutesToGame(session.getGameId(), session.getUserId(), minutes);
    }

    private void addMinutesToGame(Long gameId, Long userId, int minutes) {
        Game game = gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));
        BigDecimal added = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal current = game.getHoursPlayed() != null ? game.getHoursPlayed() : BigDecimal.ZERO;
        game.setHoursPlayed(current.add(added));
        gameRepository.save(game);
    }

    private void verifyOwnedGame(Long userId, Long gameId) {
        gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));
    }

    private PlaySessionView toView(PlaySession session, Game game) {
        String title = game != null ? game.getTitle() : "Jogo";
        return new PlaySessionView(
                session.getId(),
                session.getGameId(),
                title,
                session.getStartedAt(),
                session.getSource(),
                session.getProcessName()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

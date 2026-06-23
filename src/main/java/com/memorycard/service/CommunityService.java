package com.memorycard.service;

import com.memorycard.dto.response.CommunityCompletion;
import com.memorycard.dto.response.CommunityLeaderboardEntry;
import com.memorycard.entity.CompletionType;
import com.memorycard.entity.Game;
import com.memorycard.entity.User;
import com.memorycard.repository.GameRepository;
import com.memorycard.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommunityService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public CommunityService(GameRepository gameRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunityCompletion> recentCompletions(int limit) {
        return gameRepository.findRecentPublicCompletions(PageRequest.of(0, limit)).stream()
                .map(this::toCompletion)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityLeaderboardEntry> leaderboard(int limit) {
        return gameRepository.findCommunityLeaderboard(PageRequest.of(0, limit)).stream()
                .map(row -> new CommunityLeaderboardEntry(
                        (String) row[0],
                        row[1] instanceof Number n ? n.longValue() : 0L))
                .toList();
    }

    @Transactional
    public void updateProfile(Long userId,
                              String nick,
                              boolean communityVisible,
                              boolean shareNotesWithFriends,
                              boolean shareJournalWithFriends,
                              boolean shareScreenshotsWithFriends,
                              String raUsername,
                              String raApiKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.memorycard.exception.ResourceNotFoundException("Usuário não encontrado"));
        user.setNick(validateNick(userId, nick));
        user.setCommunityVisible(communityVisible);
        user.setShareNotesWithFriends(shareNotesWithFriends);
        user.setShareJournalWithFriends(shareJournalWithFriends);
        user.setShareScreenshotsWithFriends(shareScreenshotsWithFriends);
        user.setRetroAchievementsUsername(blankToNull(raUsername));
        if (raApiKey != null && !raApiKey.isBlank() && !raApiKey.contains("•••")) {
            user.setRetroAchievementsApiKey(raApiKey.trim());
        }
        userRepository.save(user);
    }

    private CommunityCompletion toCompletion(Game game) {
        User user = userRepository.findById(game.getUserId()).orElse(null);
        String player = user != null ? user.getDisplayNick() : "Jogador";
        String completionLabel = game.getCompletionType() != null
                ? game.getCompletionType().getLabel()
                : "Zerado";
        return new CommunityCompletion(
                player,
                game.getTitle(),
                game.getPlatform(),
                game.getCompletedAt(),
                completionLabel
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String validateNick(Long userId, String nick) {
        if (nick == null || nick.isBlank()) {
            return null;
        }
        String trimmed = nick.trim();
        if (trimmed.length() < 3 || trimmed.length() > 30) {
            throw new IllegalArgumentException("O nick deve ter entre 3 e 30 caracteres");
        }
        if (!trimmed.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("O nick só pode conter letras, números e underscore");
        }
        userRepository.findByNickIgnoreCase(trimmed).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalArgumentException("Este nick já está em uso");
            }
        });
        return trimmed;
    }
}

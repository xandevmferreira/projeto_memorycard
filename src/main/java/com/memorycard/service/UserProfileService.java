package com.memorycard.service;

import com.memorycard.dto.response.GameSummaryView;
import com.memorycard.dto.response.UserPublicProfile;
import com.memorycard.entity.Game;
import com.memorycard.entity.User;
import com.memorycard.exception.AccessDeniedException;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameRepository;
import com.memorycard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final FriendService friendService;
    private final BadgeService badgeService;
    private final CoverImageService coverImageService;

    public UserProfileService(UserRepository userRepository,
                              GameRepository gameRepository,
                              FriendService friendService,
                              BadgeService badgeService,
                              CoverImageService coverImageService) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.friendService = friendService;
        this.badgeService = badgeService;
        this.coverImageService = coverImageService;
    }

    @Transactional(readOnly = true)
    public UserPublicProfile getProfile(Long viewerId, Long profileUserId) {
        User user = userRepository.findById(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        boolean ownProfile = viewerId.equals(profileUserId);
        boolean friend = friendService.areFriends(viewerId, profileUserId);
        if (!ownProfile && !friend) {
            throw new AccessDeniedException("Você precisa ser amigo deste usuário para ver o perfil");
        }

        List<Game> games = gameRepository.findByUserIdOrderByCreatedAtDesc(profileUserId);
        long completed = gameRepository.countByUserIdAndStatus(profileUserId, com.memorycard.entity.GameStatus.COMPLETED);
        BigDecimal hours = gameRepository.sumHoursPlayedByUserId(profileUserId);

        List<GameSummaryView> summaries = games.stream()
                .map(g -> new GameSummaryView(
                        g.getId(),
                        g.getTitle(),
                        g.getPlatform(),
                        g.getStatus(),
                        toCover(g.getCoverUrl()),
                        g.getExternalRating(),
                        g.getPersonalRating()
                ))
                .toList();

        return new UserPublicProfile(
                user.getId(),
                user.getName(),
                user.getCreatedAt(),
                games.size(),
                completed,
                hours != null ? hours : BigDecimal.ZERO,
                summaries,
                ownProfile
                        ? badgeService.badgesForUser(profileUserId, true)
                        : badgeService.earnedBadgesForUser(profileUserId),
                ownProfile,
                friend
        );
    }

    private String toCover(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (coverUrl.startsWith("/uploads") || coverUrl.contains("steamstatic.com")) {
            return coverUrl;
        }
        return coverImageService.toDisplayUrl(coverUrl);
    }
}

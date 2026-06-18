package com.memorycard.service;

import com.memorycard.dto.response.BadgeView;
import com.memorycard.entity.Badge;
import com.memorycard.entity.GameStatus;
import com.memorycard.entity.User;
import com.memorycard.entity.UserBadge;
import com.memorycard.repository.BadgeRepository;
import com.memorycard.repository.FriendshipRepository;
import com.memorycard.repository.GameRepository;
import com.memorycard.repository.UserBadgeRepository;
import com.memorycard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GameRepository gameRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public BadgeService(BadgeRepository badgeRepository,
                        UserBadgeRepository userBadgeRepository,
                        GameRepository gameRepository,
                        FriendshipRepository friendshipRepository,
                        UserRepository userRepository) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.gameRepository = gameRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void evaluateAndAward(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        long totalGames = gameRepository.countByUserId(userId);
        long completed = gameRepository.countByUserIdAndStatus(userId, GameStatus.COMPLETED);
        long retroGames = gameRepository.countByUserIdAndRetroTrue(userId);
        long friends = friendshipRepository.countAcceptedFriends(userId);

        awardIfMissing(userId, "FIRST_GAME", totalGames >= 1);
        awardIfMissing(userId, "FIRST_COMPLETE", completed >= 1);
        awardIfMissing(userId, "COLLECTOR_5", totalGames >= 5);
        awardIfMissing(userId, "COLLECTOR_10", totalGames >= 10);
        awardIfMissing(userId, "COMPLETIONIST_3", completed >= 3);
        awardIfMissing(userId, "COMPLETIONIST_10", completed >= 10);
        awardIfMissing(userId, "RETRO_FAN", retroGames >= 1);
        awardIfMissing(userId, "SOCIAL_START", friends >= 1);
        awardIfMissing(userId, "SOCIAL_3", friends >= 3);
        awardIfMissing(userId, "COMMUNITY", user.isCommunityVisible());
    }

    @Transactional(readOnly = true)
    public List<BadgeView> badgesForUser(Long userId, boolean includeLocked) {
        List<Badge> all = badgeRepository.findAllByOrderBySortOrderAsc();
        Map<Long, UserBadge> earned = userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, Function.identity()));

        List<BadgeView> views = new ArrayList<>();
        for (Badge badge : all) {
            UserBadge userBadge = earned.get(badge.getId());
            boolean hasBadge = userBadge != null;
            if (hasBadge || includeLocked) {
                views.add(new BadgeView(
                        badge.getCode(),
                        badge.getName(),
                        badge.getDescription(),
                        badge.getIcon(),
                        hasBadge,
                        hasBadge ? userBadge.getEarnedAt() : null
                ));
            }
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<BadgeView> earnedBadgesForUser(Long userId) {
        return badgesForUser(userId, false).stream().filter(BadgeView::earned).toList();
    }

    private void awardIfMissing(Long userId, String code, boolean condition) {
        if (!condition) {
            return;
        }
        badgeRepository.findByCode(code).ifPresent(badge -> {
            if (!userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                UserBadge userBadge = new UserBadge();
                userBadge.setUserId(userId);
                userBadge.setBadgeId(badge.getId());
                userBadgeRepository.save(userBadge);
            }
        });
    }
}

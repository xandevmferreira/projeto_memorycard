package com.memorycard.service;

import com.memorycard.dto.response.FriendView;
import com.memorycard.entity.Friendship;
import com.memorycard.entity.FriendshipStatus;
import com.memorycard.entity.User;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.FriendshipRepository;
import com.memorycard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    public FriendService(FriendshipRepository friendshipRepository,
                         UserRepository userRepository,
                         BadgeService badgeService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.badgeService = badgeService;
    }

    @Transactional
    public void inviteByEmail(Long currentUserId, String email) {
        User target = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com este e-mail"));
        sendInvite(currentUserId, target);
    }

    @Transactional
    public void inviteByNick(Long currentUserId, String nick) {
        User target = userRepository.findByNickIgnoreCase(nick.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com este nick"));
        sendInvite(currentUserId, target);
    }

    private void sendInvite(Long currentUserId, User target) {
        if (target.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Você não pode adicionar a si mesmo");
        }

        friendshipRepository.findBetweenUsers(currentUserId, target.getId()).ifPresent(existing -> {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalArgumentException("Vocês já são amigos");
            }
            if (existing.getStatus() == FriendshipStatus.PENDING) {
                throw new IllegalArgumentException("Já existe um convite pendente entre vocês");
            }
        });

        Friendship friendship = new Friendship();
        friendship.setRequesterId(currentUserId);
        friendship.setAddresseeId(target.getId());
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void accept(Long currentUserId, Long friendshipId) {
        Friendship friendship = getIncomingPending(currentUserId, friendshipId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        badgeService.evaluateAndAward(currentUserId);
        badgeService.evaluateAndAward(friendship.getRequesterId());
    }

    @Transactional
    public void reject(Long currentUserId, Long friendshipId) {
        Friendship friendship = getIncomingPending(currentUserId, friendshipId);
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void removeFriend(Long currentUserId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Amizade não encontrada"));
        if (!friendship.getRequesterId().equals(currentUserId)
                && !friendship.getAddresseeId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Amizade não encontrada");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendView> listFriends(Long userId) {
        return friendshipRepository.findByUserAndStatus(userId, FriendshipStatus.ACCEPTED).stream()
                .map(f -> toView(f, userId, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendView> pendingIncoming(Long userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(f -> toView(f, userId, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendView> pendingOutgoing(Long userId) {
        return friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(f -> toView(f, userId, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean areFriends(Long userA, Long userB) {
        if (userA.equals(userB)) {
            return true;
        }
        return friendshipRepository.findBetweenUsers(userA, userB)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    private Friendship getIncomingPending(Long currentUserId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        if (!friendship.getAddresseeId().equals(currentUserId)
                || friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResourceNotFoundException("Convite não encontrado");
        }
        return friendship;
    }

    private FriendView toView(Friendship friendship, Long currentUserId, boolean incoming) {
        Long otherId = friendship.getRequesterId().equals(currentUserId)
                ? friendship.getAddresseeId()
                : friendship.getRequesterId();
        User other = userRepository.findById(otherId).orElse(null);
        String name = other != null ? other.getDisplayNick() : "Jogador";
        return new FriendView(
                friendship.getId(),
                otherId,
                name,
                friendship.getStatus() == FriendshipStatus.ACCEPTED ? friendship.getCreatedAt() : null,
                incoming
        );
    }
}
